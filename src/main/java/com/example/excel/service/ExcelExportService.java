package com.example.excel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.excel.dto.ExportRequest;
import com.example.excel.dto.ExportResponse;
import com.example.excel.entity.ExportTask;
import com.example.excel.entity.User;
import com.example.excel.mapper.ExportTaskMapper;
import com.example.excel.mapper.UserMapper;
import com.example.excel.util.MemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Excel导出服务
 * 实现百万级数据的高效导出
 */
@Slf4j
@Service
public class ExcelExportService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExportTaskMapper exportTaskMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${export.batch-size:10000}")
    private Integer batchSize;

    @Value("${export.temp-path:/tmp/excel/}")
    private String tempPath;

    @Value("${export.timeout:30}")
    private Integer timeoutMinutes;

    @Value("${export.max-concurrent-tasks:5}")
    private Integer maxConcurrentTasks;

    private static final String TASK_CACHE_PREFIX = "export:task:";
    private static final String PROCESSING_COUNT_KEY = "export:processing:count";

    /**
     * 启动导出任务
     *
     * @param request 导出请求
     * @return 导出响应
     */
    public ExportResponse startExport(ExportRequest request) {
        // 检查并发任务数限制
        int processingCount = exportTaskMapper.countProcessingTasks();
        if (processingCount >= maxConcurrentTasks) {
            throw new RuntimeException("当前导出任务过多，请稍后再试");
        }

        // 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");
        
        // 构建查询参数
        Map<String, Object> queryParams = buildQueryParams(request);
        
        // 统计总数据量
        Long totalCount = userMapper.countUserForExport(queryParams);
        if (totalCount == 0) {
            throw new RuntimeException("没有符合条件的数据可导出");
        }

        // 创建导出任务记录
        ExportTask task = createExportTask(taskId, request, totalCount);
        exportTaskMapper.insert(task);

        // 缓存任务信息
        cacheTaskInfo(taskId, task);

        // 异步执行导出
        if (request.getAsync()) {
            executeExportAsync(taskId, queryParams);
        } else {
            // 同步导出（小数据量）
            executeExportSync(taskId, queryParams);
        }

        return buildExportResponse(task);
    }

    /**
     * 查询导出任务状态
     *
     * @param taskId 任务ID
     * @return 导出响应
     */
    public ExportResponse getExportStatus(String taskId) {
        // 先从缓存获取
        ExportTask task = getTaskFromCache(taskId);
        if (task == null) {
            // 从数据库获取
            task = exportTaskMapper.selectByTaskId(taskId);
            if (task == null) {
                throw new RuntimeException("导出任务不存在");
            }
            // 更新缓存
            cacheTaskInfo(taskId, task);
        }

        return buildExportResponse(task);
    }

    /**
     * 异步执行导出
     *
     * @param taskId 任务ID
     * @param queryParams 查询参数
     */
    @Async
    public void executeExportAsync(String taskId, Map<String, Object> queryParams) {
        try {
            log.info("开始异步导出任务: {}", taskId);
            
            // 更新任务状态为处理中
            updateTaskStatus(taskId, "PROCESSING", null);
            
            // 执行导出
            doExport(taskId, queryParams);
            
            // 更新任务状态为成功
            updateTaskStatus(taskId, "SUCCESS", null);
            
            log.info("异步导出任务完成: {}", taskId);
            
        } catch (Exception e) {
            log.error("异步导出任务失败: {}", taskId, e);
            updateTaskStatus(taskId, "FAILED", e.getMessage());
        }
    }

    /**
     * 同步执行导出
     *
     * @param taskId 任务ID
     * @param queryParams 查询参数
     */
    public void executeExportSync(String taskId, Map<String, Object> queryParams) {
        try {
            log.info("开始同步导出任务: {}", taskId);
            
            // 更新任务状态为处理中
            updateTaskStatus(taskId, "PROCESSING", null);
            
            // 执行导出
            doExport(taskId, queryParams);
            
            // 更新任务状态为成功
            updateTaskStatus(taskId, "SUCCESS", null);
            
            log.info("同步导出任务完成: {}", taskId);
            
        } catch (Exception e) {
            log.error("同步导出任务失败: {}", taskId, e);
            updateTaskStatus(taskId, "FAILED", e.getMessage());
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 执行具体的导出逻辑
     *
     * @param taskId 任务ID
     * @param queryParams 查询参数
     */
    private void doExport(String taskId, Map<String, Object> queryParams) throws Exception {
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 创建内存监控器
        MemoryMonitor memoryMonitor = new MemoryMonitor(taskId, 50); // 每50ms监控一次
        memoryMonitor.startMonitoring();
        
        try {
            // 创建临时目录
            File tempDir = new File(tempPath);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // 生成文件名
            String fileName = generateFileName(task.getTaskName());
            String filePath = tempPath + fileName;
        
        // 使用EasyExcel进行分批写入
        try (FileOutputStream outputStream = new FileOutputStream(filePath);
             ExcelWriter excelWriter = EasyExcel.write(outputStream, User.class).build()) {
            
            WriteSheet writeSheet = EasyExcel.writerSheet("用户数据").build();
            
            Long totalCount = task.getTotalCount();
            Long processedCount = 0L;
            Long offset = 0L;
            
            // 分批处理数据
            while (processedCount < totalCount) {
                // 查询当前批次数据
                List<User> batchData = userMapper.selectUserListForExport(
                    queryParams, offset, batchSize);
                
                if (batchData.isEmpty()) {
                    break;
                }
                
                // 写入Excel
                excelWriter.write(batchData, writeSheet);
                
                // 更新进度
                processedCount += batchData.size();
                offset += batchSize;
                
                double progress = (double) processedCount / totalCount * 100;
                updateTaskProgress(taskId, processedCount, progress);
                
                log.info("任务 {} 进度: {}/{} ({}%)", 
                    taskId, processedCount, totalCount, String.format("%.2f", progress));
                
                // 智能内存管理和GC触发
                if (processedCount % (batchSize * 20) == 0) {
                    manageMemory(taskId, processedCount);
                }
            }
        }
        
            // 获取文件信息
            File file = new File(filePath);
            long fileSize = file.length();
            
            // 更新文件信息
            exportTaskMapper.updateFileInfo(taskId, filePath, fileName, fileSize);
            
            log.info("导出完成，文件路径: {}, 文件大小: {} bytes", filePath, fileSize);
            
        } finally {
            // 停止内存监控并记录统计信息
            memoryMonitor.stopMonitoring();
            MemoryMonitor.MemoryStats memoryStats = memoryMonitor.getMemoryStats();
            
            log.info("任务 {} 内存使用统计 - 开始: {:.2f} MB, 峰值: {:.2f} MB, 增长: {:.2f} MB", 
                taskId, 
                memoryStats.getStartMemoryMB(),
                memoryStats.getPeakMemoryMB(),
                memoryStats.getMemoryIncreaseMB());
        }
    }

    /**
     * 构建查询参数
     */
    private Map<String, Object> buildQueryParams(ExportRequest request) {
        Map<String, Object> params = request.getQueryParams();
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        
        if (StringUtils.hasText(request.getUsername())) {
            params.put("username", request.getUsername());
        }
        if (StringUtils.hasText(request.getDepartment())) {
            params.put("department", request.getDepartment());
        }
        if (request.getStartTime() != null) {
            params.put("startTime", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            params.put("endTime", request.getEndTime());
        }
        
        return params;
    }

    /**
     * 创建导出任务
     */
    private ExportTask createExportTask(String taskId, ExportRequest request, Long totalCount) {
        ExportTask task = new ExportTask();
        task.setTaskId(taskId);
        task.setTaskName(StringUtils.hasText(request.getTaskName()) ? 
            request.getTaskName() : "用户数据导出");
        task.setExportType(request.getExportType());
        task.setStatus("PENDING");
        task.setTotalCount(totalCount);
        task.setProcessedCount(0L);
        task.setProgress(0.0);
        task.setCreateBy(request.getCreateBy());
        task.setStartTime(LocalDateTime.now());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        return task;
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String taskName) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.xlsx", taskName, timestamp);
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, String errorMessage) {
        exportTaskMapper.updateStatus(taskId, status, errorMessage);
        
        // 更新缓存
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task != null) {
            cacheTaskInfo(taskId, task);
        }
    }

    /**
     * 更新任务进度
     */
    private void updateTaskProgress(String taskId, Long processedCount, Double progress) {
        exportTaskMapper.updateProgress(taskId, processedCount, progress);
        
        // 更新缓存
        ExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task != null) {
            cacheTaskInfo(taskId, task);
        }
    }

    /**
     * 缓存任务信息
     */
    private void cacheTaskInfo(String taskId, ExportTask task) {
        String cacheKey = TASK_CACHE_PREFIX + taskId;
        redisTemplate.opsForValue().set(cacheKey, task, timeoutMinutes, TimeUnit.MINUTES);
    }

    /**
     * 从缓存获取任务信息
     */
    private ExportTask getTaskFromCache(String taskId) {
        String cacheKey = TASK_CACHE_PREFIX + taskId;
        return (ExportTask) redisTemplate.opsForValue().get(cacheKey);
    }

    /**
     * 构建导出响应
     */
    private ExportResponse buildExportResponse(ExportTask task) {
        ExportResponse response = new ExportResponse();
        response.setTaskId(task.getTaskId());
        response.setTaskName(task.getTaskName());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setTotalCount(task.getTotalCount());
        response.setProcessedCount(task.getProcessedCount());
        response.setFileName(task.getFileName());
        response.setFileSize(task.getFileSize());
        response.setErrorMessage(task.getErrorMessage());
        response.setStartTime(task.getStartTime());
        response.setEndTime(task.getEndTime());
        response.setCreateTime(task.getCreateTime());
        
        // 如果任务完成且成功，生成下载URL
        if ("SUCCESS".equals(task.getStatus()) && StringUtils.hasText(task.getFilePath())) {
            response.setDownloadUrl("/api/export/download/" + task.getTaskId());
        }
        
        return response;
    }

    /**
     * 智能内存管理
     * 根据内存使用情况决定是否触发GC
     */
    private void manageMemory(String taskId, Long processedCount) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;
            
            log.debug("任务 {} 处理 {} 条记录，当前内存使用率: {}%", 
                taskId, processedCount, String.format("%.2f", memoryUsage * 100));
            
            // 内存使用率超过75%时触发GC
            if (memoryUsage > 0.75) {
                long beforeGC = usedMemory;
                long startTime = System.currentTimeMillis();
                
                System.gc();
                
                // 等待GC完成
                Thread.sleep(50);
                
                long afterUsed = runtime.totalMemory() - runtime.freeMemory();
                long gcTime = System.currentTimeMillis() - startTime;
                long freedMemory = beforeGC - afterUsed;
                
                log.info("任务 {} 触发GC完成，释放内存: {} MB，耗时: {} ms，内存使用率从 {}% 降至 {}%",
                    taskId, 
                    freedMemory / (1024 * 1024),
                    gcTime,
                    String.format("%.2f", memoryUsage * 100),
                    String.format("%.2f", (double) afterUsed / maxMemory * 100));
                    
            } else if (memoryUsage > 0.6) {
                // 内存使用率超过60%时记录警告
                log.warn("任务 {} 内存使用率较高: {}%，建议关注", 
                    taskId, String.format("%.2f", memoryUsage * 100));
            }
            
        } catch (Exception e) {
            log.error("内存管理过程中发生异常", e);
        }
    }
}