package com.example.excel.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统监控控制器
 * 提供内存、GC等性能监控数据
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    /**
     * 获取内存使用情况
     *
     * @return 内存监控数据
     */
    @GetMapping("/memory")
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // JVM内存信息
            Map<String, Object> jvmMemory = new HashMap<>();
            jvmMemory.put("totalMemory", runtime.totalMemory());
            jvmMemory.put("freeMemory", runtime.freeMemory());
            jvmMemory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            jvmMemory.put("maxMemory", runtime.maxMemory());
            jvmMemory.put("availableProcessors", runtime.availableProcessors());
            
            // 内存使用率
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = (double) usedMemory / runtime.maxMemory() * 100;
            jvmMemory.put("memoryUsage", memoryUsage);
            
            result.put("jvmMemory", jvmMemory);
            
            // 堆内存详细信息
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            Map<String, Object> heapInfo = new HashMap<>();
            heapInfo.put("init", heapMemory.getInit());
            heapInfo.put("used", heapMemory.getUsed());
            heapInfo.put("committed", heapMemory.getCommitted());
            heapInfo.put("max", heapMemory.getMax());
            heapInfo.put("usage", (double) heapMemory.getUsed() / heapMemory.getMax() * 100);
            
            result.put("heapMemory", heapInfo);
            
            // 非堆内存信息
            MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
            Map<String, Object> nonHeapInfo = new HashMap<>();
            nonHeapInfo.put("init", nonHeapMemory.getInit());
            nonHeapInfo.put("used", nonHeapMemory.getUsed());
            nonHeapInfo.put("committed", nonHeapMemory.getCommitted());
            nonHeapInfo.put("max", nonHeapMemory.getMax());
            
            result.put("nonHeapMemory", nonHeapInfo);
            
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取内存信息失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取GC信息
     *
     * @return GC监控数据
     */
    @GetMapping("/gc")
    public Map<String, Object> getGCInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            
            Map<String, Object> gcInfo = new HashMap<>();
            long totalCollectionCount = 0;
            long totalCollectionTime = 0;
            
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                Map<String, Object> gcDetail = new HashMap<>();
                gcDetail.put("name", gcBean.getName());
                gcDetail.put("collectionCount", gcBean.getCollectionCount());
                gcDetail.put("collectionTime", gcBean.getCollectionTime());
                gcDetail.put("memoryPoolNames", gcBean.getMemoryPoolNames());
                
                totalCollectionCount += gcBean.getCollectionCount();
                totalCollectionTime += gcBean.getCollectionTime();
                
                gcInfo.put(gcBean.getName().replaceAll("\\s+", ""), gcDetail);
            }
            
            gcInfo.put("totalCollectionCount", totalCollectionCount);
            gcInfo.put("totalCollectionTime", totalCollectionTime);
            gcInfo.put("averageCollectionTime", 
                totalCollectionCount > 0 ? (double) totalCollectionTime / totalCollectionCount : 0);
            
            result.put("gcInfo", gcInfo);
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取GC信息失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 手动触发GC
     *
     * @return 操作结果
     */
    @GetMapping("/gc/trigger")
    public Map<String, Object> triggerGC() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long beforeUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long beforeTime = System.currentTimeMillis();
            
            System.gc();
            
            // 等待一小段时间让GC完成
            Thread.sleep(100);
            
            long afterUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long afterTime = System.currentTimeMillis();
            
            result.put("beforeUsed", beforeUsed);
            result.put("afterUsed", afterUsed);
            result.put("freedMemory", beforeUsed - afterUsed);
            result.put("gcTime", afterTime - beforeTime);
            result.put("success", true);
            result.put("message", "GC触发成功");
            
            log.info("手动触发GC完成，释放内存: {} bytes，耗时: {} ms", 
                beforeUsed - afterUsed, afterTime - beforeTime);
            
        } catch (Exception e) {
            log.error("触发GC失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取系统综合信息
     *
     * @return 系统监控数据
     */
    @GetMapping("/system")
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取内存信息
            Map<String, Object> memoryInfo = getMemoryInfo();
            result.put("memory", memoryInfo.get("jvmMemory"));
            result.put("heap", memoryInfo.get("heapMemory"));
            
            // 获取GC信息
            Map<String, Object> gcInfo = getGCInfo();
            result.put("gc", gcInfo.get("gcInfo"));
            
            // 系统属性
            Map<String, Object> systemProps = new HashMap<>();
            systemProps.put("javaVersion", System.getProperty("java.version"));
            systemProps.put("javaVendor", System.getProperty("java.vendor"));
            systemProps.put("osName", System.getProperty("os.name"));
            systemProps.put("osVersion", System.getProperty("os.version"));
            systemProps.put("osArch", System.getProperty("os.arch"));
            
            result.put("system", systemProps);
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}