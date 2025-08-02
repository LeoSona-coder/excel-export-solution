package com.example.excel.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存监控器
 * 用于监控导出过程中的内存使用情况，记录峰值内存
 */
@Slf4j
@Data
public class MemoryMonitor {
    
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final AtomicLong peakMemoryUsage = new AtomicLong(0);
    private final AtomicLong startMemoryUsage = new AtomicLong(0);
    private volatile Thread monitorThread;
    private final String taskId;
    private final int monitorIntervalMs;
    
    public MemoryMonitor(String taskId) {
        this.taskId = taskId;
        this.monitorIntervalMs = 100; // 每100ms监控一次
    }
    
    public MemoryMonitor(String taskId, int monitorIntervalMs) {
        this.taskId = taskId;
        this.monitorIntervalMs = monitorIntervalMs;
    }
    
    /**
     * 开始监控内存使用情况
     */
    public void startMonitoring() {
        if (monitoring.compareAndSet(false, true)) {
            Runtime runtime = Runtime.getRuntime();
            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            startMemoryUsage.set(currentMemory);
            peakMemoryUsage.set(currentMemory);
            
            log.info("任务 {} 开始内存监控，初始内存使用: {} MB", 
                taskId, currentMemory / (1024 * 1024));
            
            monitorThread = new Thread(this::monitorMemoryUsage, "MemoryMonitor-" + taskId);
            monitorThread.setDaemon(true);
            monitorThread.start();
        }
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        if (monitoring.compareAndSet(true, false)) {
            if (monitorThread != null) {
                monitorThread.interrupt();
            }
            
            Runtime runtime = Runtime.getRuntime();
            long endMemory = runtime.totalMemory() - runtime.freeMemory();
            long startMemory = startMemoryUsage.get();
            long peakMemory = peakMemoryUsage.get();
            
            log.info("任务 {} 内存监控结束 - 开始: {} MB, 峰值: {} MB, 结束: {} MB, 峰值增长: {} MB", 
                taskId, 
                startMemory / (1024 * 1024),
                peakMemory / (1024 * 1024),
                endMemory / (1024 * 1024),
                (peakMemory - startMemory) / (1024 * 1024));
        }
    }
    
    /**
     * 内存监控循环
     */
    private void monitorMemoryUsage() {
        while (monitoring.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Runtime runtime = Runtime.getRuntime();
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                
                // 更新峰值内存
                long currentPeak = peakMemoryUsage.get();
                while (currentMemory > currentPeak) {
                    if (peakMemoryUsage.compareAndSet(currentPeak, currentMemory)) {
                        log.debug("任务 {} 内存峰值更新: {} MB", 
                            taskId, currentMemory / (1024 * 1024));
                        break;
                    }
                    currentPeak = peakMemoryUsage.get();
                }
                
                Thread.sleep(monitorIntervalMs);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("内存监控过程中发生异常", e);
            }
        }
    }
    
    /**
     * 获取内存使用统计
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        
        return new MemoryStats(
            startMemoryUsage.get(),
            peakMemoryUsage.get(),
            currentMemory,
            peakMemoryUsage.get() - startMemoryUsage.get()
        );
    }
    
    /**
     * 内存统计数据
     */
    @Data
    public static class MemoryStats {
        private final long startMemory;
        private final long peakMemory;
        private final long currentMemory;
        private final long memoryIncrease;
        
        public MemoryStats(long startMemory, long peakMemory, long currentMemory, long memoryIncrease) {
            this.startMemory = startMemory;
            this.peakMemory = peakMemory;
            this.currentMemory = currentMemory;
            this.memoryIncrease = memoryIncrease;
        }
        
        public double getStartMemoryMB() {
            return startMemory / (1024.0 * 1024.0);
        }
        
        public double getPeakMemoryMB() {
            return peakMemory / (1024.0 * 1024.0);
        }
        
        public double getCurrentMemoryMB() {
            return currentMemory / (1024.0 * 1024.0);
        }
        
        public double getMemoryIncreaseMB() {
            return memoryIncrease / (1024.0 * 1024.0);
        }
    }
}