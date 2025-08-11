package cn.ningmo.geminicraftchat.logging;

import cn.ningmo.geminicraftchat.GeminiCraftChat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * 异步日志管理器
 * 使用队列和独立线程处理日志写入，避免阻塞主线程
 */
public class AsyncLogManager {
    private final GeminiCraftChat plugin;
    private final File logDirectory;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timestampFormat;
    
    // 异步处理相关
    private final BlockingQueue<LogEntry> logQueue;
    private final ExecutorService logExecutor;
    private ScheduledExecutorService flushExecutor;
    private final AtomicBoolean running;
    private final AtomicLong droppedLogs;
    
    // 日志写入器
    private final Map<String, PrintWriter> logWriters;
    
    // 配置
    private final boolean loggingEnabled;
    private final boolean separateFiles;
    private final int queueSize;
    private final int batchSize;
    private final long flushInterval;
    
    // 性能统计
    private final AtomicLong totalLogs;
    private final AtomicLong avgProcessTime;
    
    public AsyncLogManager(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.logDirectory = new File(plugin.getDataFolder(), "logs");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 读取配置
        this.loggingEnabled = plugin.getConfigManager().getConfig().getBoolean("logging.enabled", true);
        this.separateFiles = plugin.getConfigManager().getConfig().getBoolean("logging.separate_files", true);
        this.queueSize = plugin.getConfigManager().getConfig().getInt("performance.log_queue_size", 10000);
        this.batchSize = plugin.getConfigManager().getConfig().getInt("performance.log_batch_size", 50);
        this.flushInterval = plugin.getConfigManager().getConfig().getLong("performance.log_flush_interval", 1000);
        
        // 初始化异步组件
        this.logQueue = new LinkedBlockingQueue<>(queueSize);
        this.logExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "GCC-AsyncLogger");
            thread.setDaemon(true);
            return thread;
        });
        this.running = new AtomicBoolean(false);
        this.droppedLogs = new AtomicLong(0);
        
        // 初始化其他组件
        this.logWriters = new ConcurrentHashMap<>();
        this.totalLogs = new AtomicLong(0);
        this.avgProcessTime = new AtomicLong(0);
    }
    
    /**
     * 启动异步日志系统
     */
    public void start() {
        if (!loggingEnabled) {
            plugin.getLogger().info("日志功能已禁用");
            return;
        }
        
        if (running.get()) {
            plugin.getLogger().warning("异步日志系统已经在运行");
            return;
        }
        
        try {
            initializeLogFiles();
            startLogProcessor();
            running.set(true);
            
            plugin.getLogger().info("异步日志系统已启动 - 队列大小: " + queueSize + ", 批处理大小: " + batchSize);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "启动异步日志系统失败", e);
        }
    }
    
    /**
     * 停止异步日志系统
     */
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        running.set(false);
        
        try {
            // 处理剩余的日志
            processRemainingLogs();
            
            // 关闭执行器
            logExecutor.shutdown();
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }

            // 关闭刷新线程
            if (flushExecutor != null) {
                flushExecutor.shutdown();
            }

            // 关闭所有写入器
            closeAllWriters();
            
            // 输出统计信息
            plugin.getLogger().info("异步日志系统已停止 - 总日志: " + totalLogs.get() + 
                ", 丢弃日志: " + droppedLogs.get());
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "停止异步日志系统时出错", e);
        }
    }
    
    /**
     * 异步记录日志
     */
    public void logAsync(String category, String message) {
        if (!running.get() || !loggingEnabled) {
            return;
        }
        
        LogEntry entry = new LogEntry(category, message, System.currentTimeMillis());
        
        if (!logQueue.offer(entry)) {
            // 队列满时丢弃日志并计数
            droppedLogs.incrementAndGet();
            
            // 每1000次丢弃输出一次警告
            if (droppedLogs.get() % 1000 == 0) {
                plugin.getLogger().warning("日志队列已满，已丢弃 " + droppedLogs.get() + " 条日志");
            }
        }
    }
    
    /**
     * 初始化日志文件
     */
    private void initializeLogFiles() throws IOException {
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            throw new IOException("无法创建日志目录: " + logDirectory.getAbsolutePath());
        }
        
        String timestamp = dateFormat.format(new Date());
        
        if (separateFiles) {
            // 创建分类日志文件
            initializeLogWriter("general", "general_" + timestamp + ".log");
            initializeLogWriter("chat", "chat_" + timestamp + ".log");
            initializeLogWriter("api", "api_" + timestamp + ".log");
            initializeLogWriter("performance", "performance_" + timestamp + ".log");
            initializeLogWriter("errors", "errors_" + timestamp + ".log");
            initializeLogWriter("stats", "stats_" + timestamp + ".log");
            initializeLogWriter("npc", "npc_" + timestamp + ".log");
        } else {
            // 创建统一日志文件
            initializeLogWriter("general", "log_" + timestamp + ".log");
        }
        
        // 记录会话开始
        logAsync("general", "=== 异步日志会话开始 ===");
        logAsync("general", "服务器版本: " + plugin.getServer().getVersion());
        logAsync("general", "插件版本: " + plugin.getDescription().getVersion());
        logAsync("general", "异步日志配置: 队列=" + queueSize + ", 批处理=" + batchSize + ", 刷新间隔=" + flushInterval + "ms");
    }
    
    /**
     * 初始化日志写入器
     */
    private void initializeLogWriter(String category, String filename) throws IOException {
        File logFile = new File(logDirectory, filename);
        PrintWriter writer = new PrintWriter(new FileWriter(logFile, true));
        logWriters.put(category, writer);
    }
    
    /**
     * 启动日志处理器
     */
    private void startLogProcessor() {
        logExecutor.submit(() -> {
            plugin.getLogger().info("异步日志处理器已启动");

            try {
                while (running.get()) {
                    try {
                        processBatch();
                        Thread.sleep(10); // 短暂休眠避免CPU占用过高
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "处理日志批次时出错", e);
                    }
                }

                // 处理剩余的日志
                while (!logQueue.isEmpty()) {
                    try {
                        processBatch();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "处理剩余日志时出错", e);
                        break;
                    }
                }
            } finally {
                plugin.getLogger().info("异步日志处理器已停止");
            }
        });

        // 启动定期刷新任务
        startFlushTask();
    }
    
    /**
     * 处理一批日志
     */
    private void processBatch() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        int processed = 0;

        // 如果队列为空，等待一段时间
        if (logQueue.isEmpty()) {
            LogEntry entry = logQueue.poll(500, TimeUnit.MILLISECONDS);
            if (entry != null) {
                writeLogEntry(entry);
                processed++;
            }
        } else {
            // 批量处理日志
            for (int i = 0; i < batchSize; i++) {
                LogEntry entry = logQueue.poll();
                if (entry == null) {
                    break;
                }

                writeLogEntry(entry);
                processed++;
            }
        }

        if (processed > 0) {
            totalLogs.addAndGet(processed);

            // 更新平均处理时间
            long processTime = System.currentTimeMillis() - startTime;
            long currentAvg = avgProcessTime.get();
            avgProcessTime.set(currentAvg == 0 ? processTime : (currentAvg + processTime) / 2);
        }
    }
    
    /**
     * 写入单条日志
     */
    private void writeLogEntry(LogEntry entry) {
        String category = separateFiles ? entry.getCategory() : "general";
        PrintWriter writer = logWriters.get(category);
        
        if (writer != null) {
            String timestamp = timestampFormat.format(new Date(entry.getTimestamp()));
            String logLine = String.format("[%s] [%s] %s", timestamp, entry.getCategory().toUpperCase(), entry.getMessage());
            writer.println(logLine);
        }
    }
    
    /**
     * 启动定期刷新任务
     */
    private void startFlushTask() {
        flushExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "GCC-LogFlusher");
            thread.setDaemon(true);
            return thread;
        });

        flushExecutor.scheduleAtFixedRate(() -> {
            try {
                flushAllWriters();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "刷新日志文件时出错", e);
            }
        }, flushInterval, flushInterval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 刷新所有写入器
     */
    private void flushAllWriters() {
        for (PrintWriter writer : logWriters.values()) {
            if (writer != null) {
                writer.flush();
            }
        }
    }
    
    /**
     * 处理剩余日志
     */
    private void processRemainingLogs() {
        plugin.getLogger().info("处理剩余的 " + logQueue.size() + " 条日志");
        
        LogEntry entry;
        while ((entry = logQueue.poll()) != null) {
            writeLogEntry(entry);
        }
        
        flushAllWriters();
    }
    
    /**
     * 关闭所有写入器
     */
    private void closeAllWriters() {
        logAsync("general", "=== 异步日志会话结束 ===");
        
        // 等待最后的日志写入
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        for (PrintWriter writer : logWriters.values()) {
            if (writer != null) {
                writer.close();
            }
        }
        logWriters.clear();
    }
    
    /**
     * 获取性能统计
     */
    public AsyncLogStats getStats() {
        return new AsyncLogStats(
            totalLogs.get(),
            droppedLogs.get(),
            logQueue.size(),
            avgProcessTime.get(),
            running.get()
        );
    }
    
    /**
     * 日志条目类
     */
    private static class LogEntry {
        private final String category;
        private final String message;
        private final long timestamp;
        
        public LogEntry(String category, String message, long timestamp) {
            this.category = category;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getCategory() { return category; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 异步日志统计类
     */
    public static class AsyncLogStats {
        private final long totalLogs;
        private final long droppedLogs;
        private final int queueSize;
        private final long avgProcessTime;
        private final boolean running;
        
        public AsyncLogStats(long totalLogs, long droppedLogs, int queueSize, long avgProcessTime, boolean running) {
            this.totalLogs = totalLogs;
            this.droppedLogs = droppedLogs;
            this.queueSize = queueSize;
            this.avgProcessTime = avgProcessTime;
            this.running = running;
        }
        
        public long getTotalLogs() { return totalLogs; }
        public long getDroppedLogs() { return droppedLogs; }
        public int getQueueSize() { return queueSize; }
        public long getAvgProcessTime() { return avgProcessTime; }
        public boolean isRunning() { return running; }
    }
}
