# GeminiCraftChat 日志系统使用指南

## 概述

GeminiCraftChat 插件内置了完整的日志系统，可以记录所有API调用、聊天记录、错误信息、性能数据和玩家使用统计。

## 日志配置

### 基本配置

在 `config.yml` 中的日志配置：

```yaml
logging:
  enabled: true                    # 启用/禁用日志系统
  directory: "logs"                # 日志文件目录
  format: "yyyy-MM-dd_HH-mm-ss"   # 日志文件名时间格式
  separate_files: true             # 是否分别保存不同类型的日志
  retention_days: 30               # 日志保留天数（自动清理）
```

### 日志类型配置

```yaml
logging:
  include:
    chat: true                    # 聊天记录
    commands: true                # 命令执行记录
    errors: true                  # 错误记录
    model_changes: true           # 模型切换记录
    temperature_changes: true     # 温度调整记录
    api_calls: true              # API调用详情
    performance: true            # 性能指标
    statistics: true             # 使用统计
```

### 高级配置

```yaml
logging:
  # 文件大小限制
  max_file_size: "10MB"          # 单个文件最大大小
  max_files: 100                 # 每种类型最大文件数
  
  # 性能设置
  async_logging: true            # 启用异步日志记录
  buffer_size: 1000             # 日志缓冲区大小
  flush_interval: 5             # 缓冲区刷新间隔（秒）
  
  # 隐私设置
  anonymize_players: false       # 在日志中匿名化玩家名称
  include_ip_addresses: false    # 在日志中包含IP地址
  
  # 压缩
  compress_old_logs: true        # 压缩7天以上的日志
  compression_format: "gzip"     # 压缩格式（gzip/zip）
```

## 日志文件结构

### 目录结构
```
plugins/GeminiCraftChat/logs/
├── chat/
│   ├── 2024-01-15_chat.log
│   ├── 2024-01-16_chat.log
│   └── ...
├── api/
│   ├── 2024-01-15_api.log
│   ├── 2024-01-16_api.log
│   └── ...
├── errors/
│   ├── 2024-01-15_errors.log
│   └── ...
├── statistics/
│   ├── daily_stats_2024-01-15.json
│   ├── player_stats.json
│   └── ...
└── archived/
    ├── 2024-01-01_chat.log.gz
    └── ...
```

### 日志文件格式

#### 聊天日志
```
[2024-01-15 14:30:25] [CHAT] 玩家: Steve | 模型: api1 | 人设: default
[2024-01-15 14:30:25] [USER] Steve: 你好AI！
[2024-01-15 14:30:27] [AI] 你好！我今天能为你做些什么？
[2024-01-15 14:30:27] [RESPONSE_TIME] 2.3s
```

#### API调用日志
```
[2024-01-15 14:30:25] [API_REQUEST] 模型: api1 | 玩家: Steve
URL: https://api.openai.com/v1/chat/completions
Headers: {"Content-Type": "application/json", "Authorization": "Bearer sk-***"}
Body: {"model": "gpt-3.5-turbo", "messages": [...], "temperature": 0.7}

[2024-01-15 14:30:27] [API_RESPONSE] 状态: 200 | 时间: 2.3s
Response: {"choices": [{"message": {"content": "你好！我今天能为你做些什么？"}}]}
```

#### 错误日志
```
[2024-01-15 14:35:12] [ERROR] 玩家: Steve | 模型: api1
错误: API请求失败: 429 Too Many Requests
堆栈跟踪:
  at GeminiService.sendGenericRequest(GeminiService.java:205)
  at GeminiService.sendMessage(GeminiService.java:108)
  ...
```

#### 性能日志
```
[2024-01-15 14:30:00] [PERFORMANCE] 内存: 512MB/2GB | CPU: 15% | 活跃玩家: 23
[2024-01-15 14:30:00] [METRICS] API调用/分钟: 45 | 平均响应时间: 2.1s | 成功率: 98.5%
```

## 日志管理命令

### 查看统计信息
```bash
/gcc logs stats
```
显示当前使用统计：
- 总API调用次数
- 成功/失败率
- 平均响应时间
- 最活跃玩家
- 模型使用分布

### 导出统计信息
```bash
/gcc logs export
```
将详细统计导出到CSV文件：
- `player_stats.csv` - 每个玩家的使用统计
- `model_stats.csv` - 每个模型的性能统计
- `daily_stats.csv` - 每日使用趋势

### 重置统计信息
```bash
/gcc logs reset
```
重置所有累积的统计计数器。

### 查看最近错误
```bash
/gcc logs errors
```
显示最近10条带时间戳的错误消息。

### 归档旧日志
```bash
/gcc logs archive
```
手动归档超过配置保留期的日志。

### 清理日志
```bash
/gcc logs cleanup
```
删除超过保留期的日志并压缩归档。

## 统计和分析

### 玩家统计
```json
{
  "player_name": "Steve",
  "total_messages": 156,
  "total_api_calls": 156,
  "successful_calls": 152,
  "failed_calls": 4,
  "average_response_time": 2.1,
  "favorite_model": "api1",
  "favorite_persona": "teacher",
  "first_use": "2024-01-01T10:00:00Z",
  "last_use": "2024-01-15T14:30:25Z",
  "total_tokens_used": 45678
}
```

### 模型性能统计
```json
{
  "model_name": "api1",
  "display_name": "OpenAI GPT-3.5",
  "total_calls": 1234,
  "successful_calls": 1198,
  "failed_calls": 36,
  "success_rate": 97.1,
  "average_response_time": 2.3,
  "min_response_time": 0.8,
  "max_response_time": 15.2,
  "total_tokens": 567890,
  "average_tokens_per_call": 474
}
```

### 每日使用趋势
```json
{
  "date": "2024-01-15",
  "total_calls": 234,
  "unique_players": 45,
  "peak_hour": 14,
  "peak_calls_per_hour": 67,
  "models_used": {
    "api1": 123,
    "api2": 89,
    "api3": 22
  },
  "average_response_time": 2.1
}
```

## 监控和警报

### 性能监控
日志系统自动跟踪：
- **响应时间**: 每个模型的平均、最小、最大响应时间
- **成功率**: API调用成功/失败率
- **资源使用**: 内存和CPU使用情况
- **玩家活动**: 活跃玩家和使用模式

### 错误跟踪
自动错误分类：
- **API错误**: 连接超时、速率限制、认证失败
- **配置错误**: 无效设置、缺少API密钥
- **插件错误**: 内部错误、内存问题
- **玩家错误**: 无效命令、权限问题

### 警报条件
配置警报：
- 高错误率（>5%失败）
- 慢响应时间（>10秒平均）
- 高内存使用（>80%堆）
- API速率限制警告

## 日志分析工具

### 内置分析命令
```bash
# 查看使用量最高的玩家
/gcc logs top players

# 查看模型性能比较
/gcc logs compare models

# 查看每小时使用模式
/gcc logs usage hourly

# 查看错误趋势
/gcc logs errors trend
```

### 外部分析
日志文件设计兼容：
- **ELK Stack**（Elasticsearch、Logstash、Kibana）
- **Splunk**
- **Grafana** 与日志数据源
- **自定义脚本** 用于CSV/JSON分析

### 示例分析查询

#### 查找高使用量玩家
```bash
grep "CHAT" logs/chat/*.log | cut -d'|' -f2 | sort | uniq -c | sort -nr | head -10
```

#### 计算平均响应时间
```bash
grep "RESPONSE_TIME" logs/chat/*.log | awk '{print $3}' | sed 's/s//' | awk '{sum+=$1; count++} END {print sum/count}'
```

#### 查找错误模式
```bash
grep "ERROR" logs/errors/*.log | cut -d':' -f3- | sort | uniq -c | sort -nr
```

## 隐私和合规

### 数据保护
- **玩家匿名化**: 选择哈希玩家名称
- **消息内容**: 可配置包含聊天内容
- **IP地址记录**: 可选IP地址记录
- **数据保留**: 配置期后自动清理

### GDPR合规
- **被遗忘权**: 删除特定玩家数据的命令
- **数据导出**: 以机器可读格式导出玩家数据
- **同意跟踪**: 记录玩家数据处理同意
- **审计跟踪**: 所有操作的完整审计跟踪

### 安全性
- **日志文件权限**: 限制日志文件访问
- **API密钥保护**: 日志中屏蔽API密钥
- **加密**: 可选日志文件加密
- **审计跟踪**: 所有操作的完整审计跟踪

## 故障排除

### 常见问题

**Q: 日志没有创建？**
A: 检查：
1. config.yml中启用了日志记录
2. 插件对日志目录有写权限
3. 磁盘空间可用
4. 服务器日志中没有文件系统错误

**Q: 日志文件太大？**
A: 配置：
1. 减少retention_days
2. 设置max_file_size限制
3. 启用日志压缩
4. 排除详细日志类型

**Q: 日志记录影响性能？**
A: 优化：
1. 启用async_logging
2. 增加buffer_size
3. 减少flush_interval
4. 禁用不必要的日志类型

**Q: 找不到特定事件？**
A: 使用：
1. Grep命令进行文本搜索
2. 检查正确的日志文件类型
3. 验证时间戳格式
4. 启用调试模式获取更多详情

### 调试日志
启用调试模式进行详细日志记录：
```yaml
debug:
  enabled: true
  log_level: "DEBUG"
  include_stack_traces: true
```

这将记录：
- 详细的API请求/响应数据
- 配置加载步骤
- 内部插件操作
- 性能指标

## 最佳实践

### 配置
1. **启用基本日志**: 始终启用chat、errors和api_calls
2. **设置合理保留期**: 根据使用情况30-90天
3. **使用压缩**: 为旧日志启用压缩
4. **监控磁盘空间**: 设置磁盘空间监控

### 性能
1. **异步日志**: 高流量服务器始终启用
2. **缓冲设置**: 根据使用情况调整缓冲区大小
3. **分离文件**: 为不同日志类型使用分离文件
4. **定期清理**: 设置自动日志清理

### 安全
1. **限制访问**: 限制日志文件访问给管理员
2. **匿名化数据**: 考虑匿名化玩家数据
3. **安全存储**: 在安全、备份的存储上存储日志
4. **定期审计**: 定期审查日志以发现安全问题

### 分析
1. **定期审查**: 每周审查日志以发现问题
2. **趋势分析**: 跟踪随时间的使用趋势
3. **性能监控**: 监控响应时间和错误率
4. **容量规划**: 使用日志进行服务器容量规划

---

**全面的日志记录，获得更好的洞察！📊📋**
