# GeminiCraftChat 日志系统使用指南

> **注意**: 这是中文版文档。[English version](docs/en/LOGGING_GUIDE.md) | [返回文档首页](docs/README.md)

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
    api_calls: true               # API调用记录
    api_requests: true            # 详细的API请求内容
    api_responses: true           # 详细的API响应内容
    performance: true             # 性能统计
    player_stats: true            # 玩家使用统计
```

### 详细程度设置

```yaml
logging:
  detail_level:
    api_requests: "full"          # full/headers_only/minimal
    api_responses: "content_only" # full/content_only/minimal
    performance: "summary"        # full/summary/minimal
```

## 日志文件结构

### 分离文件模式 (separate_files: true)

当启用分离文件模式时，会创建以下日志文件：

- `general_YYYY-MM-DD_HH-mm-ss.log` - 一般日志（命令、模型切换等）
- `chat_YYYY-MM-DD_HH-mm-ss.log` - 聊天记录
- `api_YYYY-MM-DD_HH-mm-ss.log` - API调用详细记录
- `performance_YYYY-MM-DD_HH-mm-ss.log` - 性能数据
- `errors_YYYY-MM-DD_HH-mm-ss.log` - 错误记录
- `stats_YYYY-MM-DD_HH-mm-ss.log` - 统计信息

### 统一文件模式 (separate_files: false)

所有日志都记录在单个文件中：
- `log_YYYY-MM-DD_HH-mm-ss.log`

## 日志内容示例

### API调用记录

```
[2025-07-03 11:30:15.123] [API调用] 玩家:Steve 模型:API接口1 URL:https://api.openai.com/v1/chat/completions 响应时间:1250ms 状态:成功
[2025-07-03 11:30:15.124] [API请求] 玩家:Steve 模型:API接口1
[2025-07-03 11:30:15.124]   URL: https://api.openai.com/v1/chat/completions
[2025-07-03 11:30:15.124]   请求头:
[2025-07-03 11:30:15.124]     Content-Type: application/json
[2025-07-03 11:30:15.124]     Authorization: Bearer ***
[2025-07-03 11:30:15.124]   请求体: {"model":"gpt-3.5-turbo","messages":[...],"temperature":0.7}
[2025-07-03 11:30:16.375] [API响应] 玩家:Steve 模型:API接口1 状态码:200
[2025-07-03 11:30:16.375]   AI回复: 你好！我是AI助手，很高兴为你服务。
```

### 聊天记录

```
[2025-07-03 11:30:15.120] [聊天] Steve > 你好
[2025-07-03 11:30:16.375] [回复] 你好！我是AI助手，很高兴为你服务。
```

### 性能统计

```
[2025-07-03 11:35:00.000] [性能汇总] 总调用:100 成功率:98.00% 平均响应时间:1150ms
```

### 玩家统计

```
[2025-07-03 12:00:00.000] === 玩家使用统计 ===
[2025-07-03 12:00:00.000] 玩家 Steve: 25 次调用
[2025-07-03 12:00:00.000] 玩家 Alex: 18 次调用
[2025-07-03 12:00:00.000] === 模型使用统计 ===
[2025-07-03 12:00:00.000] 模型 api1: 30 次使用
[2025-07-03 12:00:00.000] 模型 api2: 13 次使用
```

## 命令使用

### 查看统计信息

```
/gcc logs stats
```

显示当前的日志统计信息，包括：
- 总API调用次数
- 总错误次数
- 活跃玩家数
- 使用的模型数
- 成功率
- 平均响应时间
- 日志配置状态

### 重置统计数据

```
/gcc logs reset
```

清空所有统计计数器，重新开始统计。

### 导出玩家统计

```
/gcc logs export
```

将当前的玩家使用统计和模型使用统计导出到日志文件中。

## 权限要求

- 查看日志统计：`gcc.admin`
- 重置统计数据：`gcc.admin`
- 导出统计信息：`gcc.admin`

## 自动清理

系统会根据 `retention_days` 配置自动清理过期的日志文件。在插件启动时会检查并删除超过保留天数的日志文件。

## 性能影响

- 日志系统使用异步写入，对游戏性能影响极小
- 可以通过调整 `detail_level` 来控制日志详细程度
- 建议在生产环境中使用 `minimal` 或 `summary` 模式

## 故障排除

### 日志文件未创建

1. 检查 `logging.enabled` 是否为 `true`
2. 确认插件有写入权限
3. 查看控制台是否有错误信息

### 日志文件过大

1. 调整 `retention_days` 减少保留天数
2. 设置 `detail_level` 为 `minimal`
3. 禁用不需要的日志类型

### 统计数据不准确

1. 使用 `/gcc logs reset` 重置统计
2. 检查是否有多个服务器实例
3. 确认配置文件中的日志设置正确

## 最佳实践

1. **生产环境**：使用 `separate_files: true` 便于分析
2. **开发环境**：启用所有日志类型和 `full` 详细模式
3. **定期备份**：重要的日志文件应定期备份
4. **监控磁盘空间**：设置合适的 `retention_days` 避免磁盘满
5. **性能优化**：根据需要调整日志详细程度

## 日志分析

可以使用以下工具分析日志：

- `grep` 命令过滤特定内容
- `awk` 统计数据
- 日志分析工具如 ELK Stack
- 自定义脚本分析性能趋势

示例：
```bash
# 统计某玩家的调用次数
grep "玩家:Steve" api_*.log | wc -l

# 查看错误记录
grep "错误" errors_*.log

# 分析响应时间
grep "响应时间" api_*.log | awk '{print $8}' | sort -n
```

---

**全面的日志记录，获得更好的洞察！📊📋**

> **完整文档**: [English](docs/en/LOGGING_GUIDE.md) | [中文](docs/zh/LOGGING_GUIDE.md) | [返回文档首页](docs/README.md)
