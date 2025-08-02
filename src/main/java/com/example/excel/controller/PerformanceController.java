package com.example.excel.controller;

import com.example.excel.dto.ExportRequest;
import com.example.excel.service.ExcelExportService;
import com.example.excel.service.TraditionalExportService;
import com.example.excel.mapper.UserMapper;
import com.example.excel.util.MemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能对比控制器
 * 用于测试和比较不同导出方式的性能
 */
@Slf4j
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    @Autowired
    private ExcelExportService excelExportService;
    
    @Autowired
    private TraditionalExportService traditionalExportService;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 性能对比测试
     * 
     * @param request 导出请求
     * @return 对比结果
     */
    @PostMapping("/compare")
    public Map<String, Object> performanceCompare(@RequestBody ExportRequest request) {
        Map<String, Object> result = new HashMap<>(); 
        
        try {
            log.info("开始性能对比测试，参数: {}", request);
            
            // 构建查询参数
            Map<String, Object> queryParams = buildQueryParams(request);
            
            // 先统计数据量
            Long totalCount = userMapper.countUserForExport(queryParams);
            log.info("测试数据量: {} 条", totalCount);
            
            if (totalCount == 0) {
                result.put("success", false);
                result.put("message", "没有符合条件的数据");
                return result;
            }
            
            // 如果数据量过大，限制测试范围
            if (totalCount > 100000) {
                log.warn("数据量过大({} 条)，为避免系统负载过高，建议使用较小的数据集进行测试", totalCount);
                result.put("warning", "数据量较大，测试可能耗时较长，建议使用较小数据集");
            }
            
            // 记录测试开始时的系统状态
            Map<String, Object> initialMemory = traditionalExportService.getMemoryInfo();
            
            // 串行执行测试，避免相互影响
            log.info("开始串行性能测试，先执行优化方案测试");
            
            // 清理环境，确保测试的独立性
            cleanupEnvironment();
            
            // 第一步：执行优化方案测试
            Map<String, Object> optimizedResult;
            try {
                optimizedResult = testOptimizedExport(request, queryParams);
                log.info("优化方案测试完成");
            } catch (Exception e) {
                log.error("优化导出测试失败", e);
                optimizedResult = new HashMap<>();
                optimizedResult.put("success", false);
                optimizedResult.put("error", e.getMessage());
            }
            
            // 等待一段时间并清理环境，确保测试隔离
            Thread.sleep(2000);
            cleanupEnvironment();
            
            log.info("开始传统方案测试");
            
            // 第二步：执行传统方案测试
            Map<String, Object> traditionalResult;
            try {
                traditionalResult = traditionalExportService.exportTraditional(queryParams);
                log.info("传统方案测试完成");
            } catch (Exception e) {
                log.error("传统导出测试失败", e);
                traditionalResult = new HashMap<>();
                traditionalResult.put("success", false);
                traditionalResult.put("error", e.getMessage());
            }
            
            // 记录测试结束时的系统状态
            Map<String, Object> finalMemory = traditionalExportService.getMemoryInfo();
            
            // 构建对比结果
            result.put("success", true);
            result.put("testDataCount", totalCount);
            result.put("initialMemory", initialMemory);
            result.put("finalMemory", finalMemory);
            result.put("optimizedExport", optimizedResult);
            result.put("traditionalExport", traditionalResult);
            
            // 计算性能对比
            if (optimizedResult.containsKey("performance") && traditionalResult.containsKey("performance")) {
                Map<String, Object> comparison = calculateComparison(
                    (Map<String, Object>) optimizedResult.get("performance"),
                    (Map<String, Object>) traditionalResult.get("performance")
                );
                result.put("comparison", comparison);
            }
            
            log.info("性能对比测试完成");
            
        } catch (Exception e) {
            log.error("性能对比测试失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试优化导出方式
     */
    private Map<String, Object> testOptimizedExport(ExportRequest request, Map<String, Object> queryParams) {
        Map<String, Object> result = new HashMap<>();
        MemoryMonitor testMemoryMonitor = null;
        
        try {
            long startTime = System.currentTimeMillis();
            
            log.info("开始优化导出测试");
            
            // 创建测试专用的内存监控器
            String testTaskId = "test-optimized-" + System.currentTimeMillis();
            testMemoryMonitor = new MemoryMonitor(testTaskId, 50);
            testMemoryMonitor.startMonitoring();
            
            // 使用优化的导出服务（同步方式，便于测试）
            request.setAsync(false); // 设置为同步导出
            com.example.excel.dto.ExportResponse exportResponse = excelExportService.startExport(request);
            String taskId = exportResponse.getTaskId();
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            // 停止内存监控并获取统计数据
            testMemoryMonitor.stopMonitoring();
            MemoryMonitor.MemoryStats memoryStats = testMemoryMonitor.getMemoryStats();
            
            // 获取任务信息
            com.example.excel.dto.ExportResponse taskInfo = excelExportService.getExportStatus(taskId);
            
            result.put("success", true);
            result.put("method", "optimized");
            result.put("taskId", taskId);
            
            if (taskInfo != null) {
                result.put("recordCount", taskInfo.getTotalCount());
                result.put("fileName", taskInfo.getFileName());
                result.put("fileSize", taskInfo.getFileSize());
            }
            
            // 性能指标
            Map<String, Object> performance = new HashMap<>();
            performance.put("totalTime", totalTime);
            performance.put("memoryUsage", (long) memoryStats.getMemoryIncrease());
            performance.put("peakMemoryUsage", memoryStats.getPeakMemory());
            performance.put("startMemoryUsage", memoryStats.getStartMemory());
            
            if (result.containsKey("recordCount")) {
                int recordCount = ((Number) result.get("recordCount")).intValue();
                performance.put("avgTimePerRecord", (double) totalTime / recordCount);
                performance.put("avgMemoryPerRecord", memoryStats.getMemoryIncreaseMB() / recordCount);
            }
            
            result.put("performance", performance);
            
            log.info("优化导出测试完成 - 耗时: {} ms, 峰值内存: {:.2f} MB, 内存增长: {:.2f} MB", 
                totalTime, memoryStats.getPeakMemoryMB(), memoryStats.getMemoryIncreaseMB());
                
        } catch (Exception e) {
            log.error("优化导出测试失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            // 确保内存监控器被正确停止
            if (testMemoryMonitor != null) {
                try {
                    testMemoryMonitor.stopMonitoring();
                } catch (Exception e) {
                    log.warn("停止内存监控时发生异常", e);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 计算性能对比
     */
    private Map<String, Object> calculateComparison(Map<String, Object> optimized, Map<String, Object> traditional) {
        Map<String, Object> comparison = new HashMap<>();
        
        try {
            // 时间对比
            long optimizedTime = ((Number) optimized.get("totalTime")).longValue();
            long traditionalTime = ((Number) traditional.get("totalTime")).longValue();
            
            double timeImprovement = ((double) (traditionalTime - optimizedTime) / traditionalTime) * 100;
            comparison.put("timeImprovement", timeImprovement);
            comparison.put("timeRatio", (double) traditionalTime / optimizedTime);
            
            // 内存对比
            long optimizedMemory = ((Number) optimized.getOrDefault("memoryUsage", 0)).longValue();
            long traditionalMemory = ((Number) traditional.getOrDefault("totalMemoryUsage", 0)).longValue();
            
            if (traditionalMemory > 0) {
                double memoryImprovement = ((double) (traditionalMemory - optimizedMemory) / traditionalMemory) * 100;
                comparison.put("memoryImprovement", memoryImprovement);
                comparison.put("memoryRatio", (double) traditionalMemory / Math.max(optimizedMemory, 1));
            }
            
            // 峰值内存对比
            long optimizedPeak = ((Number) optimized.getOrDefault("peakMemoryUsage", 0)).longValue();
            long traditionalPeak = ((Number) traditional.getOrDefault("peakMemoryUsage", 0)).longValue();
            
            if (traditionalPeak > 0) {
                double peakMemoryImprovement = ((double) (traditionalPeak - optimizedPeak) / traditionalPeak) * 100;
                comparison.put("peakMemoryImprovement", peakMemoryImprovement);
                comparison.put("peakMemoryRatio", (double) traditionalPeak / Math.max(optimizedPeak, 1));
            }
            
            // 每条记录平均时间对比
            if (optimized.containsKey("avgTimePerRecord") && traditional.containsKey("avgTimePerRecord")) {
                double optimizedAvgTime = ((Number) optimized.get("avgTimePerRecord")).doubleValue();
                double traditionalAvgTime = ((Number) traditional.get("avgTimePerRecord")).doubleValue();
                
                double avgTimeImprovement = ((traditionalAvgTime - optimizedAvgTime) / traditionalAvgTime) * 100;
                comparison.put("avgTimeImprovement", avgTimeImprovement);
            }
            
            // 总结
            Map<String, Object> summary = new HashMap<>();
            summary.put("优化方案更快", timeImprovement > 0 ? String.format("%.1f%%", timeImprovement) : "否");
            summary.put("内存使用更少", comparison.containsKey("memoryImprovement") && 
                ((Number) comparison.get("memoryImprovement")).doubleValue() > 0 ? 
                String.format("%.1f%%", ((Number) comparison.get("memoryImprovement")).doubleValue()) : "否");
            summary.put("推荐方案", timeImprovement > 0 || 
                (comparison.containsKey("memoryImprovement") && 
                 ((Number) comparison.get("memoryImprovement")).doubleValue() > 0) ? "优化方案" : "传统方案");
            
            comparison.put("summary", summary);
            
        } catch (Exception e) {
            log.error("计算性能对比失败", e);
            comparison.put("error", "计算对比数据失败: " + e.getMessage());
        }
        
        return comparison;
    }
    
    /**
     * 构建查询参数
     */
    private Map<String, Object> buildQueryParams(ExportRequest request) {
        Map<String, Object> params = request.getQueryParams();
        if (params == null) {
            params = new HashMap<>();
        }
        
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            params.put("username", request.getUsername());
        }
        if (request.getDepartment() != null && !request.getDepartment().trim().isEmpty()) {
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
     * 获取系统资源使用情况
     */
    @GetMapping("/system-info")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            Map<String, Object> memory = new HashMap<>();
            memory.put("totalMemory", runtime.totalMemory());
            memory.put("freeMemory", runtime.freeMemory());
            memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            memory.put("maxMemory", runtime.maxMemory());
            memory.put("availableProcessors", runtime.availableProcessors());
            
            result.put("memory", memory);
            result.put("timestamp", System.currentTimeMillis());
            result.put("success", true);
            
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 清理测试环境
     * 确保每次测试的独立性和准确性
     */
    private void cleanupEnvironment() {
        try {
            log.info("开始清理测试环境");
            
            // 记录清理前的内存状态
            Runtime runtime = Runtime.getRuntime();
            long beforeCleanup = runtime.totalMemory() - runtime.freeMemory();
            
            // 强制垃圾回收，清理之前测试的内存影响
            System.gc();
            Thread.sleep(100); // 等待GC完成
            System.gc(); // 再次GC确保彻底清理
            Thread.sleep(100);
            
            // 记录清理后的内存状态
            long afterCleanup = runtime.totalMemory() - runtime.freeMemory();
            long freedMemory = beforeCleanup - afterCleanup;
            
            log.info("环境清理完成，释放内存: {} MB，当前内存使用: {} MB", 
                freedMemory / (1024 * 1024), afterCleanup / (1024 * 1024));
                
        } catch (Exception e) {
            log.warn("环境清理过程中发生异常，但不影响测试继续", e);
        }
    }
}