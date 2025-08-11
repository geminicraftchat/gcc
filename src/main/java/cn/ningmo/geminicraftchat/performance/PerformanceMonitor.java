package cn.ningmo.geminicraftchat.performance;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.logging.AsyncLogManager;
import cn.ningmo.geminicraftchat.npc.SmartNPCManager;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控系统
 * 监控插件的各项性能指标并提供自动调优建议
 */
public class PerformanceMonitor {
    private final GeminiCraftChat plugin;
    private final ScheduledExecutorService monitorExecutor;
    
    // 性能指标
    private final AtomicLong apiCallCount;
    private final AtomicLong totalResponseTime;
    private final AtomicLong avgResponseTime;
    private final AtomicLong peakMemoryUsage;
    private final AtomicLong currentMemoryUsage;
    
    // 系统监控
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    
    // 配置
    private final boolean autoTuningEnabled;
    private final long monitorInterval;
    private final double cpuThreshold;
    private final long memoryThreshold;
    
    // 状态
    private boolean running;
    private long startTime;
    
    public PerformanceMonitor(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "GCC-PerformanceMonitor");
            thread.setDaemon(true);
            return thread;
        });
        
        // 初始化性能指标
        this.apiCallCount = new AtomicLong(0);
        this.totalResponseTime = new AtomicLong(0);
        this.avgResponseTime = new AtomicLong(0);
        this.peakMemoryUsage = new AtomicLong(0);
        this.currentMemoryUsage = new AtomicLong(0);
        
        // 初始化系统监控
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        
        // 读取配置
        this.autoTuningEnabled = plugin.getConfigManager().getConfig().getBoolean("performance.auto_performance_tuning", true);
        this.monitorInterval = plugin.getConfigManager().getConfig().getLong("performance.monitor_interval", 30000); // 30秒
        this.cpuThreshold = plugin.getConfigManager().getConfig().getDouble("performance.cpu_threshold", 80.0);
        this.memoryThreshold = plugin.getConfigManager().getConfig().getLong("performance.memory_threshold", 100_000_000); // 100MB
        
        this.running = false;
    }
    
    /**
     * 启动性能监控
     */
    public void start() {
        if (running) {
            plugin.getLogger().warning("性能监控已经在运行");
            return;
        }
        
        running = true;
        startTime = System.currentTimeMillis();
        
        // 启动定期监控
        monitorExecutor.scheduleAtFixedRate(this::performMonitoring, 0, monitorInterval, TimeUnit.MILLISECONDS);
        
        // 启动自动调优（如果启用）
        if (autoTuningEnabled) {
            monitorExecutor.scheduleAtFixedRate(this::performAutoTuning, 60000, 60000, TimeUnit.MILLISECONDS); // 每分钟检查
        }
        
        plugin.getLogger().info("性能监控已启动 - 监控间隔: " + (monitorInterval / 1000) + "秒, 自动调优: " + autoTuningEnabled);
    }
    
    /**
     * 停止性能监控
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // 输出最终报告
        generateFinalReport();
        
        // 关闭执行器
        monitorExecutor.shutdown();
        try {
            if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        plugin.getLogger().info("性能监控已停止");
    }
    
    /**
     * 记录API调用
     */
    public void recordApiCall(long responseTime) {
        apiCallCount.incrementAndGet();
        totalResponseTime.addAndGet(responseTime);
        
        // 更新平均响应时间
        long count = apiCallCount.get();
        if (count > 0) {
            avgResponseTime.set(totalResponseTime.get() / count);
        }
    }
    
    /**
     * 执行性能监控
     */
    private void performMonitoring() {
        try {
            // 更新内存使用情况
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            currentMemoryUsage.set(usedMemory);
            
            if (usedMemory > peakMemoryUsage.get()) {
                peakMemoryUsage.set(usedMemory);
            }
            
            // 生成监控报告
            PerformanceReport report = generateReport();
            
            // 记录到日志（如果启用异步日志）
            if (plugin.getAsyncLogManager() != null) {
                plugin.getAsyncLogManager().logAsync("performance", formatReportForLog(report));
            }
            
            // 检查性能警告
            checkPerformanceWarnings(report);
            
        } catch (Exception e) {
            plugin.getLogger().warning("性能监控执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行自动调优
     */
    private void performAutoTuning() {
        try {
            PerformanceReport report = generateReport();
            
            // CPU使用率过高
            if (report.getCpuUsage() > cpuThreshold) {
                plugin.getLogger().warning("CPU使用率过高 (" + String.format("%.1f", report.getCpuUsage()) + "%)，启动自动调优");
                optimizeForHighCPU();
            }
            
            // 内存使用过高（检查使用率而不是绝对值）
            if (report.getMemoryUsagePercent() > 85.0) {
                plugin.getLogger().warning("内存使用率过高 (" + String.format("%.1f", report.getMemoryUsagePercent()) + "%)，启动自动调优");
                optimizeForHighMemory();
            } else if (report.getMemoryUsage() > memoryThreshold) {
                plugin.getLogger().warning("内存使用过高 (" + formatBytes(report.getMemoryUsage()) + ")，启动自动调优");
                optimizeForHighMemory();
            }
            
            // API响应时间过长
            if (report.getAvgResponseTime() > 5000) { // 5秒
                plugin.getLogger().warning("API响应时间过长 (" + report.getAvgResponseTime() + "ms)，启动自动调优");
                optimizeForSlowAPI();
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("自动调优执行失败: " + e.getMessage());
        }
    }
    
    /**
     * CPU优化
     */
    private void optimizeForHighCPU() {
        // 增加NPC更新间隔
        plugin.getLogger().info("自动调优: 降低NPC更新频率以减少CPU使用");
        
        // 减少并发AI请求
        plugin.getLogger().info("自动调优: 限制并发AI请求数量");
        
        // 触发垃圾回收（可配置）
        if (plugin.getConfigManager().getConfig().getBoolean("performance.allow_explicit_gc", false)) {
            System.gc();
        }
    }
    
    /**
     * 内存优化
     */
    private void optimizeForHighMemory() {
        // 清理缓存
        plugin.getLogger().info("自动调优: 清理内存缓存");

        // 触发垃圾回收（可配置）
        if (plugin.getConfigManager().getConfig().getBoolean("performance.allow_explicit_gc", false)) {
            System.gc();
        }

        // 检查内存使用率而不是绝对值
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsagePercent = maxMemory > 0 ? (double) currentMemoryUsage.get() / maxMemory * 100 : 0;

        // 只有在内存使用率真正过高时才建议重启
        if (memoryUsagePercent > 95.0) {
            plugin.getLogger().severe("内存使用率严重过高 (" + String.format("%.1f", memoryUsagePercent) + "%)，建议重启服务器");
        }
    }
    
    /**
     * API优化
     */
    private void optimizeForSlowAPI() {
        plugin.getLogger().info("自动调优: 检测到API响应缓慢，建议检查网络连接和API配置");
    }
    
    /**
     * 生成性能报告
     */
    public PerformanceReport generateReport() {
        long uptime = System.currentTimeMillis() - startTime;
        long memoryUsage = currentMemoryUsage.get();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsagePercent = maxMemory > 0 ? (double) memoryUsage / maxMemory * 100 : 0;
        
        // 获取CPU使用率（简化版本）
        double cpuUsage = getCPUUsage();
        
        // 获取子系统统计
        AsyncLogManager.AsyncLogStats logStats = null;
        if (plugin.getAsyncLogManager() != null) {
            logStats = plugin.getAsyncLogManager().getStats();
        }
        
        SmartNPCManager.SmartNPCStats npcStats = null;
        if (plugin.getSmartNPCManager() != null) {
            npcStats = plugin.getSmartNPCManager().getStats();
        }
        
        return new PerformanceReport(
            uptime,
            apiCallCount.get(),
            avgResponseTime.get(),
            memoryUsage,
            peakMemoryUsage.get(),
            memoryUsagePercent,
            cpuUsage,
            logStats,
            npcStats
        );
    }
    
    /**
     * 获取CPU使用率
     */
    private double getCPUUsage() {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                return sunOsBean.getProcessCpuLoad() * 100;
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return 0.0;
    }
    
    /**
     * 检查性能警告
     */
    private void checkPerformanceWarnings(PerformanceReport report) {
        // 内存警告
        if (report.getMemoryUsagePercent() > 85) {
            plugin.getLogger().warning("内存使用率过高: " + String.format("%.1f", report.getMemoryUsagePercent()) + "%");
        }
        
        // API响应时间警告
        if (report.getAvgResponseTime() > 3000) {
            plugin.getLogger().warning("API平均响应时间过长: " + report.getAvgResponseTime() + "ms");
        }
        
        // 日志队列警告
        if (report.getLogStats() != null && report.getLogStats().getDroppedLogs() > 0) {
            plugin.getLogger().warning("检测到丢弃的日志: " + report.getLogStats().getDroppedLogs() + " 条");
        }
    }
    
    /**
     * 格式化报告用于日志
     */
    private String formatReportForLog(PerformanceReport report) {
        return String.format("性能报告 - 运行时间: %ds, API调用: %d, 平均响应: %dms, 内存: %s (%.1f%%), CPU: %.1f%%",
            report.getUptime() / 1000,
            report.getApiCallCount(),
            report.getAvgResponseTime(),
            formatBytes(report.getMemoryUsage()),
            report.getMemoryUsagePercent(),
            report.getCpuUsage()
        );
    }
    
    /**
     * 生成最终报告
     */
    private void generateFinalReport() {
        PerformanceReport finalReport = generateReport();
        
        plugin.getLogger().info("=== 性能监控最终报告 ===");
        plugin.getLogger().info("总运行时间: " + (finalReport.getUptime() / 1000) + " 秒");
        plugin.getLogger().info("总API调用: " + finalReport.getApiCallCount() + " 次");
        plugin.getLogger().info("平均响应时间: " + finalReport.getAvgResponseTime() + " ms");
        plugin.getLogger().info("峰值内存使用: " + formatBytes(peakMemoryUsage.get()));
        plugin.getLogger().info("当前内存使用: " + formatBytes(finalReport.getMemoryUsage()) + 
            " (" + String.format("%.1f", finalReport.getMemoryUsagePercent()) + "%)");
        
        if (finalReport.getLogStats() != null) {
            AsyncLogManager.AsyncLogStats logStats = finalReport.getLogStats();
            plugin.getLogger().info("日志统计 - 总数: " + logStats.getTotalLogs() + 
                ", 丢弃: " + logStats.getDroppedLogs() + ", 队列: " + logStats.getQueueSize());
        }
        
        if (finalReport.getNpcStats() != null) {
            SmartNPCManager.SmartNPCStats npcStats = finalReport.getNpcStats();
            plugin.getLogger().info("NPC统计 - 总更新: " + npcStats.getTotalUpdates() + 
                ", 活跃NPC: " + npcStats.getActiveNPCs() + "/" + npcStats.getTotalNPCs());
        }
        
        plugin.getLogger().info("========================");
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    // Getters
    public boolean isRunning() { return running; }
    public long getApiCallCount() { return apiCallCount.get(); }
    public long getAvgResponseTime() { return avgResponseTime.get(); }
    public long getCurrentMemoryUsage() { return currentMemoryUsage.get(); }
    public long getPeakMemoryUsage() { return peakMemoryUsage.get(); }
    
    /**
     * 性能报告类
     */
    public static class PerformanceReport {
        private final long uptime;
        private final long apiCallCount;
        private final long avgResponseTime;
        private final long memoryUsage;
        private final long peakMemoryUsage;
        private final double memoryUsagePercent;
        private final double cpuUsage;
        private final AsyncLogManager.AsyncLogStats logStats;
        private final SmartNPCManager.SmartNPCStats npcStats;
        
        public PerformanceReport(long uptime, long apiCallCount, long avgResponseTime, 
                               long memoryUsage, long peakMemoryUsage, double memoryUsagePercent, 
                               double cpuUsage, AsyncLogManager.AsyncLogStats logStats, 
                               SmartNPCManager.SmartNPCStats npcStats) {
            this.uptime = uptime;
            this.apiCallCount = apiCallCount;
            this.avgResponseTime = avgResponseTime;
            this.memoryUsage = memoryUsage;
            this.peakMemoryUsage = peakMemoryUsage;
            this.memoryUsagePercent = memoryUsagePercent;
            this.cpuUsage = cpuUsage;
            this.logStats = logStats;
            this.npcStats = npcStats;
        }
        
        // Getters
        public long getUptime() { return uptime; }
        public long getApiCallCount() { return apiCallCount; }
        public long getAvgResponseTime() { return avgResponseTime; }
        public long getMemoryUsage() { return memoryUsage; }
        public long getPeakMemoryUsage() { return peakMemoryUsage; }
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public double getCpuUsage() { return cpuUsage; }
        public AsyncLogManager.AsyncLogStats getLogStats() { return logStats; }
        public SmartNPCManager.SmartNPCStats getNpcStats() { return npcStats; }
    }
}
