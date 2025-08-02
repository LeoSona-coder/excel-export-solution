package com.example.excel.service;

import com.alibaba.excel.EasyExcel;
import com.example.excel.entity.User;
import com.example.excel.mapper.UserMapper;
import com.example.excel.util.MemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传统Excel导出服务
 * 用于性能对比测试
 */
@Slf4j
@Service
public class TraditionalExportService {

    @Autowired
    private UserMapper userMapper;

    @Value("${export.temp-path:/tmp/excel/}")
    private String tempPath;

    /**
     * 传统方式导出（一次性加载所有数据）
     * 
     * @param queryParams 查询参数
     * @return 导出结果
     */
    public Map<String, Object> exportTraditional(Map<String, Object> queryParams) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new java.util.HashMap<>();
        
        // 创建内存监控器
        String taskId = "traditional-export-" + System.currentTimeMillis();
        MemoryMonitor memoryMonitor = new MemoryMonitor(taskId, 50);
        memoryMonitor.startMonitoring();
        
        try {
            log.info("开始传统方式导出，查询参数: {}", queryParams);
            
            // 一次性查询所有数据（这是传统方式的问题所在）
            log.info("开始查询所有数据...");
            long queryStartTime = System.currentTimeMillis();
            
            List<User> allUsers = userMapper.selectUserListForExport(queryParams, 0L, Integer.MAX_VALUE);
            
            long queryEndTime = System.currentTimeMillis();
            long queryTime = queryEndTime - queryStartTime;
            
            log.info("数据查询完成，共 {} 条记录，耗时: {} ms", allUsers.size(), queryTime);
            
            // 创建临时目录
            File tempDir = new File(tempPath);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // 生成文件名
            String fileName = "traditional_export_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            String filePath = tempPath + fileName;
            
            // 一次性写入Excel（传统方式）
            log.info("开始写入Excel文件...");
            long writeStartTime = System.currentTimeMillis();
            
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                EasyExcel.write(outputStream, User.class)
                    .sheet("用户数据")
                    .doWrite(allUsers);
            }
            
            long writeEndTime = System.currentTimeMillis();
            long writeTime = writeEndTime - writeStartTime;
            
            log.info("Excel写入完成，耗时: {} ms", writeTime);
            
            // 获取文件信息
            File file = new File(filePath);
            long fileSize = file.length();
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            // 停止内存监控并获取统计数据
            memoryMonitor.stopMonitoring();
            MemoryMonitor.MemoryStats memoryStats = memoryMonitor.getMemoryStats();
            
            // 构建结果
            result.put("success", true);
            result.put("method", "traditional");
            result.put("recordCount", allUsers.size());
            result.put("fileName", fileName);
            result.put("fileSize", fileSize);
            
            // 性能指标
            Map<String, Object> performance = new HashMap<>();
            performance.put("totalTime", totalTime);
            performance.put("queryTime", queryTime);
            performance.put("writeTime", writeTime);
            performance.put("memoryUsage", memoryStats.getMemoryIncrease());
            performance.put("totalMemoryUsage", memoryStats.getPeakMemory());
            performance.put("peakMemoryUsage", memoryStats.getPeakMemory());
            performance.put("startMemoryUsage", memoryStats.getStartMemory());
            performance.put("avgTimePerRecord", (double) totalTime / allUsers.size());
            performance.put("avgMemoryPerRecord", memoryStats.getMemoryIncreaseMB() / allUsers.size());
            
            result.put("performance", performance);
            
            log.info("传统导出完成 - 总耗时: {} ms, 查询耗时: {} ms, 写入耗时: {} ms, 峰值内存: {:.2f} MB, 内存增长: {:.2f} MB", 
                totalTime, queryTime, writeTime, memoryStats.getPeakMemoryMB(), memoryStats.getMemoryIncreaseMB());
                
        } catch (Exception e) {
            log.error("传统导出失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            // 确保内存监控器被正确停止
            try {
                memoryMonitor.stopMonitoring();
            } catch (Exception e) {
                log.warn("停止内存监控时发生异常", e);
            }
        }
        
        return result;
    }
    
    /**
     * 获取内存使用情况
     */
    public Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memoryInfo = new java.util.HashMap<>();
        
        memoryInfo.put("totalMemory", runtime.totalMemory());
        memoryInfo.put("freeMemory", runtime.freeMemory());
        memoryInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        memoryInfo.put("maxMemory", runtime.maxMemory());
        memoryInfo.put("memoryUsage", (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100);
        
        return memoryInfo;
    }
}