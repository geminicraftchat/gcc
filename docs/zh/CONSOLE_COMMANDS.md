# GeminiCraftChat 控制台命令指南

## 概述

GeminiCraftChat 插件现在完全支持控制台命令执行。管理员可以在服务器控制台中使用所有插件命令，无需进入游戏。

## 控制台权限

- **控制台默认拥有所有管理员权限**
- 无需配置额外权限，控制台可以执行所有命令
- 某些特定于玩家的功能会有相应提示

## 可用命令

### 基础管理命令

#### 重新加载配置
```
gcc reload
```
- 重新加载插件配置文件
- 应用新的设置而无需重启服务器

#### 清除对话历史
```
gcc clear all
```
- 清除所有玩家的对话历史记录
- 控制台无法清除单个玩家的历史记录

#### 切换调试模式
```
gcc debug
```
- 启用/禁用调试模式
- 显示详细的日志信息

### 模型管理命令

#### 列出可用模型
```
gcc model list
gcc models
```
- 显示所有已配置的API模型
- 显示模型名称和显示名称

#### 切换当前模型
```
gcc model switch <模型名称>
gcc model <模型名称>
```
- 切换到指定模型
- 示例：`gcc model api2`

#### 显示当前模型
```
gcc model info
gcc model current
```
- 显示当前活动模型
- 显示模型配置详情

#### 显示模型详情
```
gcc model info <模型名称>
```
- 显示特定模型的详细信息
- 显示API配置、超时设置等

### 温度管理

#### 显示当前温度
```
gcc temp
gcc temperature
```
- 显示当前模型温度设置

#### 设置温度
```
gcc temp <数值>
gcc temperature <数值>
```
- 设置模型温度（0.0-1.0）
- 示例：`gcc temp 0.8`

### 人设管理命令

#### 列出所有人设
```
gcc persona list
```
- 显示所有可用人设
- 显示人设名称和描述

#### 显示人设详情
```
gcc persona info <人设名称>
```
- 显示详细人设信息
- 显示系统提示和配置

#### 设置玩家人设（仅管理员）
```
gcc persona set <玩家名称> <人设名称>
```
- 为玩家设置特定人设
- 示例：`gcc persona set Steve teacher`

### 超时管理命令（长思考功能）

#### 列出所有模型超时设置
```
gcc timeout list
```
- 显示所有模型的超时设置
- 显示连接、读取、写入超时和长思考状态

#### 显示模型超时详情
```
gcc timeout info <模型名称>
```
- 显示特定模型的详细超时信息
- 示例：`gcc timeout info api2`

#### 切换长思考模式
```
gcc timeout toggle <模型名称>
```
- 启用/禁用特定模型的长思考模式
- 示例：`gcc timeout toggle api2`

#### 设置自定义超时（高级）
```
gcc timeout set <模型名称> <类型> <秒数>
```
- 设置特定超时值
- 类型：connect、read、write
- 示例：`gcc timeout set api2 read 300`

### 日志命令

#### 查看统计信息
```
gcc logs stats
gcc statistics
```
- 显示使用统计信息
- 显示API调用次数、响应时间等

#### 重置统计信息
```
gcc logs reset
```
- 重置所有统计计数器
- 清除累积数据

#### 导出玩家统计
```
gcc logs export
```
- 将玩家使用统计导出到文件
- 在logs目录中创建CSV文件

#### 查看最近错误
```
gcc logs errors
```
- 显示最近的错误消息
- 显示最近10个带时间戳的错误

### 玩家管理命令

#### 清除特定玩家历史
```
gcc clear player <玩家名称>
```
- 清除特定玩家的聊天历史
- 示例：`gcc clear player Steve`

#### 显示玩家信息
```
gcc player info <玩家名称>
```
- 显示玩家的当前设置
- 显示人设、聊天历史数量等

#### 列出活跃玩家
```
gcc players
gcc player list
```
- 显示所有使用过AI聊天的玩家
- 显示最后活动时间

### 系统信息命令

#### 显示插件状态
```
gcc status
gcc info
```
- 显示插件状态和信息
- 显示版本、加载的模型、活动连接

#### 显示配置摘要
```
gcc config
```
- 显示当前配置摘要
- 显示关键设置和值

#### 测试API连接
```
gcc test <模型名称>
gcc test
```
- 测试特定模型（或当前模型）的API连接
- 发送测试请求以验证连接性

### 高级命令

#### 强制垃圾回收
```
gcc gc
```
- 强制Java垃圾回收
- 帮助释放内存

#### 显示内存使用
```
gcc memory
```
- 显示当前内存使用情况
- 显示堆使用和可用内存

#### 备份配置
```
gcc backup
```
- 创建当前配置的备份
- 保存到带时间戳的backups目录

## 命令示例

### 日常管理
```bash
# 检查插件状态
gcc status

# 查看当前模型和设置
gcc model info
gcc temp

# 检查最近活动
gcc logs stats
gcc players

# 测试API连接性
gcc test
```

### 模型管理
```bash
# 列出所有可用模型
gcc models

# 切换到不同模型
gcc model api2

# 调整温度以获得更好响应
gcc temp 0.7

# 检查超时设置
gcc timeout list
```

### 长思考管理
```bash
# 查看所有超时设置
gcc timeout list

# 为复杂模型启用长思考
gcc timeout toggle api2

# 检查特定模型超时详情
gcc timeout info api2

# 为研究任务设置自定义超时
gcc timeout set api2 read 600
```

### 故障排除
```bash
# 启用调试模式
gcc debug

# 检查最近错误
gcc logs errors

# 测试API连接
gcc test api1

# 更改后重新加载配置
gcc reload
```

### 玩家支持
```bash
# 帮助玩家解决人设问题
gcc persona list
gcc persona set PlayerName teacher

# 清除玩家卡住的聊天历史
gcc clear player PlayerName

# 检查玩家当前设置
gcc player info PlayerName
```

## 输出示例

### 模型列表输出
```
可用模型：
- api1: OpenAI GPT-3.5 (当前)
- api2: Claude Sonnet
- api3: Google Gemini
- api4: DeepSeek R1
- api5: 自定义API
```

### 超时列表输出
```
模型超时设置：
api1 (OpenAI GPT-3.5):
  连接: 30秒 | 读取: 60秒 | 写入: 30秒 | 长思考: 关闭
api2 (Claude Sonnet):
  连接: 30秒 | 读取: 180秒 | 写入: 30秒 | 长思考: 开启
api3 (Google Gemini):
  连接: 30秒 | 读取: 90秒 | 写入: 30秒 | 长思考: 开启
```

### 统计信息输出
```
GeminiCraftChat 统计信息：
总API调用: 1,234
成功调用: 1,198 (97.1%)
失败调用: 36 (2.9%)
平均响应时间: 2.3秒
最常用模型: api1 (45.2%)
活跃玩家: 23
```

## 最佳实践

### 定期维护
1. **每日检查统计**: `gcc logs stats`
2. **监控错误**: `gcc logs errors`
3. **测试API连接性**: `gcc test`
4. **备份配置**: `gcc backup`

### 性能优化
1. **监控内存使用**: `gcc memory`
2. **根据使用情况调整超时**: `gcc timeout list`
3. **清除旧统计**: `gcc logs reset`（每月）

### 玩家支持
1. **检查玩家问题**: `gcc player info <名称>`
2. **重置卡住的对话**: `gcc clear player <名称>`
3. **为用户调整人设**: `gcc persona set <名称> <人设>`

### 模型管理
1. **根据负载切换模型**: `gcc model <名称>`
2. **调整温度以提高质量**: `gcc temp <数值>`
3. **为复杂任务启用长思考**: `gcc timeout toggle <模型>`

## 故障排除

### 常见问题

**Q: 控制台命令不工作？**
A: 确保使用准确的命令语法，不要加`/`前缀。

**Q: 无法切换模型？**
A: 检查模型是否在config.yml中正确配置并重新加载插件。

**Q: 超时命令不可用？**
A: 更新到包含长思考功能的最新版本。

**Q: 统计信息不显示？**
A: 在config.yml中启用日志记录并重启插件。

### 调试信息

启用调试模式以查看详细的命令执行：
```bash
gcc debug
```

这将显示：
- 命令处理详情
- API调用信息
- 配置加载状态
- 错误堆栈跟踪

## 安全注意事项

- **控制台访问**: 只有受信任的管理员应该有控制台访问权限
- **API密钥**: 永远不要在控制台输出中显示API密钥
- **玩家数据**: 管理玩家特定数据时要小心
- **备份**: 建议定期备份配置

---

**高效服务器管理！🖥️⚡**
