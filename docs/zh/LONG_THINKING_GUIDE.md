# GeminiCraftChat 长思考功能指南

## 概述

长思考功能允许AI模型花费更多时间进行深度思考，特别适合处理复杂问题、创意写作、代码分析等需要深度推理的任务。每个API接口都可以独立配置超时设置和长思考模式。

## 功能特点

### ✨ 主要特性

1. **独立配置**：每个API接口都有独立的超时设置
2. **灵活切换**：可以动态开启/关闭长思考模式
3. **智能超时**：根据模型特性配置不同的超时时间
4. **实时管理**：通过命令实时查看和管理超时设置
5. **日志记录**：所有超时相关操作都会被记录

### 🔧 技术实现

- **多客户端架构**：为每个模型创建专用的HTTP客户端
- **动态超时**：根据配置动态调整连接、读取、写入超时
- **内存优化**：客户端复用和智能缓存机制
- **优雅关闭**：确保所有客户端正确关闭

## 配置说明

### 基础配置结构

```yaml
api:
  models:
    api1:
      name: "GPT-3.5 Turbo"
      model: "gpt-3.5-turbo"
      base_url: "https://api.openai.com/v1/chat/completions"
      api_key: "your-api-key-here"
      max_tokens: 4096
      temperature: 0.7
      # 超时设置（秒）
      timeout:
        connect: 30           # 连接超时
        read: 60             # 读取超时
        write: 30            # 写入超时
        long_thinking: false # 长思考模式开关
```

### 超时参数详解

#### connect（连接超时）
- **作用**：建立HTTP连接的最大等待时间
- **推荐值**：30秒
- **范围**：10-60秒
- **注意**：太短可能导致连接失败；太长可能阻塞其他请求

#### read（读取超时）
- **作用**：等待API响应的最大时间
- **推荐值**：60-300秒（取决于模型）
- **范围**：30-600秒
- **注意**：这是长思考最重要的设置

#### write（写入超时）
- **作用**：发送请求数据的最大时间
- **推荐值**：30秒
- **范围**：10-60秒
- **注意**：通常不需要调整，除非网络很慢

#### long_thinking（长思考模式）
- **作用**：启用/禁用扩展思考模式
- **值**：true/false
- **效果**：启用时自动使用更长的读取超时
- **默认**：false

## 按模型类型推荐

### 快速模型（GPT-3.5，基础API）
```yaml
timeout:
  connect: 30
  read: 60
  write: 30
  long_thinking: false
```
**适用场景**：快速响应、简单问答、基础对话

### 平衡模型（GPT-4，Claude Haiku）
```yaml
timeout:
  connect: 30
  read: 120
  write: 30
  long_thinking: true
```
**适用场景**：中等复杂度任务、详细解释、代码审查

### 思考模型（Claude Opus，DeepSeek R1）
```yaml
timeout:
  connect: 30
  read: 300
  write: 30
  long_thinking: true
```
**适用场景**：复杂推理、创意写作、深度分析

### 研究模型（o1-preview，o1-mini）
```yaml
timeout:
  connect: 30
  read: 600
  write: 30
  long_thinking: true
```
**适用场景**：研究任务、复杂问题解决、学术分析

## 命令使用

### 查看所有模型超时设置
```bash
/gcc timeout list
```
**输出示例**：
```
模型超时设置：
api1 (GPT-3.5 Turbo):
  连接: 30秒 | 读取: 60秒 | 写入: 30秒 | 长思考: 关闭
api2 (Claude Sonnet):
  连接: 30秒 | 读取: 180秒 | 写入: 30秒 | 长思考: 开启
api3 (DeepSeek R1):
  连接: 30秒 | 读取: 300秒 | 写入: 30秒 | 长思考: 开启
```

### 查看特定模型超时详情
```bash
/gcc timeout info api2
```
**输出示例**：
```
模型: api2 (Claude Sonnet)
当前超时设置：
  连接超时: 30秒
  读取超时: 180秒
  写入超时: 30秒
  长思考模式: 已启用
  
最后修改: 2024-01-15 14:30:25
状态: 活跃
最近性能:
  平均响应时间: 45.2秒
  成功率: 98.5%
  超时错误: 2次（最近24小时）
```

### 切换长思考模式
```bash
/gcc timeout toggle api2
```
**输出示例**：
```
✅ api2 (Claude Sonnet) 的长思考模式已启用
新的读取超时: 180秒
```

### 设置自定义超时（高级）
```bash
/gcc timeout set api2 read 300
```
**输出示例**：
```
✅ api2 (Claude Sonnet) 的读取超时已设置为300秒
长思考模式: 已启用
```

## 使用场景

### 1. 创意写作
**推荐设置**：
```yaml
timeout:
  read: 240
  long_thinking: true
```
**模型**：Claude Opus、GPT-4、DeepSeek R1
**命令**：
- `/gcc timeout toggle claude_opus`
- `/gcc model claude_opus`

### 2. 代码分析
**推荐设置**：
```yaml
timeout:
  read: 180
  long_thinking: true
```
**模型**：GPT-4、Claude Sonnet、DeepSeek R1
**命令**：
- `/gcc timeout set api2 read 180`
- `/gcc model api2`

### 3. 复杂问题解决
**推荐设置**：
```yaml
timeout:
  read: 600
  long_thinking: true
```
**模型**：o1-preview、o1-mini、Claude Opus
**命令**：
- `/gcc timeout set o1_preview read 600`
- `/gcc model o1_preview`

### 4. 快速问答
**推荐设置**：
```yaml
timeout:
  read: 60
  long_thinking: false
```
**模型**：GPT-3.5、Claude Haiku
**命令**：
- `/gcc timeout toggle gpt35 # 禁用长思考`
- `/gcc model gpt35`

## 性能监控

### 实时监控
```bash
# 检查当前性能
/gcc logs stats

# 查看最近超时
/gcc logs errors

# 监控特定模型
/gcc timeout info api2
```

### 性能指标
系统跟踪：
- **平均响应时间**：每个模型的响应时间
- **超时率**：超时请求的百分比
- **成功率**：总体API调用成功率
- **峰值响应时间**：记录的最长响应时间

### 优化建议

#### 如果经常超时：
1. **增加读取超时**：`/gcc timeout set <模型> read <秒数>`
2. **启用长思考**：`/gcc timeout toggle <模型>`
3. **切换到更快模型**：`/gcc model <更快模型>`
4. **检查网络连接**：`/gcc test <模型>`

#### 如果响应太慢：
1. **减少读取超时**：`/gcc timeout set <模型> read <秒数>`
2. **禁用长思考**：`/gcc timeout toggle <模型>`
3. **切换到更快模型**：`/gcc model <更快模型>`
4. **调整温度**：`/gcc temp 0.3`（更低=更快）

## 最佳实践

### 配置
1. **保守开始**：从较短超时开始，根据需要增加
2. **模型特定**：根据模型特性配置超时
3. **监控性能**：定期检查超时统计
4. **测试更改**：用实际使用测试超时更改

### 使用
1. **匹配任务复杂度**：复杂任务使用更长超时
2. **考虑用户体验**：平衡响应质量与等待时间
3. **高峰时段**：高流量期间考虑较短超时
4. **备用模型**：准备更快的模型作为备选

### 故障排除
1. **启用调试模式**：`/gcc debug` 获取详细超时信息
2. **检查日志**：审查错误日志中的超时模式
3. **测试连接性**：使用 `/gcc test <模型>` 验证API访问
4. **监控资源**：检查服务器内存和CPU使用

## 高级配置

### 动态超时调整
```yaml
# 示例：基于服务器负载的自动超时缩放
timeout:
  connect: 30
  read: 120
  write: 30
  long_thinking: true
  # 高级设置
  auto_scale: true          # 启用自动缩放
  scale_factor: 1.5         # 启用时将超时乘以此因子
  max_timeout: 600          # 允许的最大超时
  min_timeout: 30           # 允许的最小超时
```

### 负载均衡与超时
```yaml
# 示例：负载均衡的不同超时策略
api1:  # 快速模型用于快速响应
  timeout:
    read: 60
    long_thinking: false
    
api2:  # 平衡模型用于中等复杂度
  timeout:
    read: 180
    long_thinking: true
    
api3:  # 慢速模型用于复杂任务
  timeout:
    read: 600
    long_thinking: true
```

## 故障排除

### 常见问题

**Q: 模型一直超时？**
A: 
1. 增加读取超时：`/gcc timeout set <模型> read 300`
2. 启用长思考：`/gcc timeout toggle <模型>`
3. 检查API状态和网络连接
4. 考虑切换到更快的模型

**Q: 响应太慢？**
A:
1. 减少读取超时：`/gcc timeout set <模型> read 60`
2. 禁用长思考：`/gcc timeout toggle <模型>`
3. 切换到更快模型：`/gcc model <更快模型>`
4. 降低温度：`/gcc temp 0.3`

**Q: 超时命令不工作？**
A:
1. 确保有管理员权限
2. 检查模型名称是否正确：`/gcc models`
3. 验证插件已更新到最新版本
4. 检查控制台错误消息

**Q: 长思考模式不生效？**
A:
1. 重新加载插件：`/gcc reload`
2. 检查config.yml中的配置语法
3. 验证模型配置正确
4. 启用调试模式：`/gcc debug`

### 调试信息

启用调试模式获取详细超时信息：
```bash
/gcc debug
```

这将显示：
- 详细的超时配置加载
- HTTP客户端创建和配置
- 请求/响应时间信息
- 超时错误详情和堆栈跟踪

---

**深度思考，成就更多！🧠⚡**
