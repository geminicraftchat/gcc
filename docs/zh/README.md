# GeminiCraftChat

一个强大的Minecraft AI聊天插件，支持多种AI模型和高级功能。

## 🌟 功能特点

### 核心功能
- **多AI模型支持** - Gemini、Claude、GPT、DeepSeek等
- **自定义触发词** - 可配置的聊天触发器
- **人设系统** - 在不同AI人格之间切换
- **敏感词过滤** - 内置内容过滤系统
- **权限系统** - 完整的权限管理
- **详细日志** - 完整的活动跟踪

### 高级功能
- **长思考模式** - 为复杂AI推理提供延长超时
- **控制台命令** - 从服务器控制台完全管理
- **5个可配置API** - 完全可自定义的API接口
- **实时切换** - 即时更改模型和设置
- **聊天历史** - 持久的对话记忆
- **代理支持** - HTTP/SOCKS代理兼容

## 🚀 快速开始

### 安装
1. 从发布页面下载最新的`geminicraftchat-x.x.x.jar`
2. 将JAR文件放入服务器的`plugins`文件夹
3. 启动/重启服务器以生成配置文件
4. 在`plugins/GeminiCraftChat/config.yml`中配置API密钥
5. 使用`/gcc reload`重新加载配置
6. 使用`ai 你好！`开始聊天

### 基础配置
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
  current_model: "api1"
```

## 💬 使用方法

### 聊天命令
- `ai <消息>` - 与AI对话
- `@ai <消息>` - 替代触发器
- `!ai <消息>` - 替代触发器

### 基础命令
- `/gcc model <名称>` - 切换AI模型
- `/gcc temp <数值>` - 调整模型温度(0.0-1.0)
- `/gcc persona list` - 显示所有可用人设
- `/gcc persona switch <名称>` - 切换到指定人设
- `/gcc clear` - 清除你的对话历史

### 管理员命令
- `/gcc reload` - 重新加载配置
- `/gcc clear all` - 清除所有玩家的对话历史
- `/gcc debug` - 切换调试模式
- `/gcc timeout list` - 查看所有模型的超时设置
- `/gcc timeout info <模型>` - 查看模型的详细超时信息
- `/gcc timeout toggle <模型>` - 切换模型的长思考模式

### 中文自然语言命令
- `清除记忆` - 清除你的对话历史
- `切换人设 <名称>` - 切换到指定人设
- `查看人设` - 显示所有可用人设
- `帮助` - 显示帮助信息

## 🔐 权限

| 权限 | 描述 | 默认值 |
|------|------|--------|
| `gcc.use` | 允许使用基本功能 | `true` |
| `gcc.admin` | 允许使用管理员命令 | `op` |
| `gcc.model.switch` | 允许切换AI模型 | `op` |
| `gcc.temperature.adjust` | 允许调整模型温度 | `op` |
| `gcc.broadcast.receive` | 允许接收AI对话广播 | `true` |
| `gcc.broadcast.bypass` | 允许跳过AI对话广播 | `op` |

## ⚙️ 配置

### API模型
插件支持5个完全可配置的API接口：

```yaml
api:
  models:
    api1:
      name: "OpenAI GPT"
      model: "gpt-3.5-turbo"
      base_url: "https://api.openai.com/v1/chat/completions"
      api_key: "your-openai-key"
      timeout:
        connect: 30
        read: 60
        write: 30
        long_thinking: false
    
    api2:
      name: "Claude Sonnet"
      model: "claude-3-sonnet-20240229"
      base_url: "https://api.anthropic.com/v1/messages"
      api_key: "your-anthropic-key"
      timeout:
        connect: 30
        read: 180
        write: 30
        long_thinking: true
```

### 长思考功能
为每个模型配置不同的超时设置：

- **connect**: 连接超时（秒）
- **read**: 响应超时（秒）
- **write**: 写入超时（秒）
- **long_thinking**: 启用扩展思考模式

推荐超时设置：
- **快速模型**（GPT-3.5）：60秒读取超时
- **平衡模型**（GPT-4、Claude）：120-180秒读取超时
- **思考模型**（Claude Opus、DeepSeek R1）：300-600秒读取超时

### 聊天设置
```yaml
chat:
  trigger: "ai"
  trigger_words: ["@ai", "!ai"]
  max_history: 10
  cooldown: 10000
  format:
    thinking: "§7[AI] §f正在思考中..."
    response: "§7[AI] §f%s"
    error: "§c[AI] 错误：%s"
```

### 日志配置
```yaml
logging:
  enabled: true
  directory: "logs"
  format: "yyyy-MM-dd_HH-mm-ss"
  include:
    chat: true
    commands: true
    errors: true
    api_calls: true
    model_changes: true
```

## 🧠 长思考模式

长思考功能允许AI模型花费更多时间进行复杂推理：

### 优势
- **更好的质量**：更深思熟虑和详细的回答
- **复杂问题**：处理数学证明、代码分析、创意写作
- **灵活配置**：每个模型独立的超时设置
- **实时管理**：无需重启即可开关

### 使用方法
```bash
# 查看所有模型超时设置
/gcc timeout list

# 查看特定模型详情
/gcc timeout info api2

# 切换长思考模式
/gcc timeout toggle api2
```

### 最佳实践
1. **用于复杂任务**：为处理困难问题的模型启用
2. **监控性能**：检查响应时间并相应调整
3. **平衡质量与速度**：选择合适的超时值
4. **网络考虑**：考虑网络延迟因素

## 🎭 人设系统

创建不同的AI人格：

```yaml
personas:
  default:
    name: "助手"
    description: "一个有用的AI助手"
    system_prompt: "你是一个有用的助手。"
  
  teacher:
    name: "老师"
    description: "一个教育AI老师"
    system_prompt: "你是一个知识渊博的老师，能够清楚地解释概念。"
  
  friend:
    name: "朋友"
    description: "一个随意、友好的AI"
    system_prompt: "你是一个随意、友好的伙伴。"
```

## 🛡️ 敏感词过滤

配置内容过滤：

```yaml
filter:
  enabled: true
  words:
    - "敏感词1"
    - "敏感词2"
  replacement: "***"
```

## 🌐 代理支持

配置HTTP/SOCKS代理：

```yaml
api:
  http_proxy:
    enabled: true
    host: "127.0.0.1"
    port: 7890
    type: "SOCKS"  # 或 "HTTP"
```

## 📊 日志系统

插件提供全面的日志记录：

### 日志类别
- **聊天日志**：所有AI对话
- **命令日志**：命令使用跟踪
- **API日志**：API请求/响应详情
- **错误日志**：错误跟踪和调试
- **统计信息**：使用统计和性能指标

### 日志命令
```bash
/gcc logs stats    # 查看统计信息
/gcc logs reset    # 重置统计信息
/gcc logs export   # 导出玩家统计
```

## 🔧 控制台命令

从服务器控制台完全管理：

```bash
# 模型管理
gcc model list
gcc model switch api2
gcc model info api1

# 玩家管理
gcc clear player PlayerName
gcc persona set PlayerName teacher

# 系统管理
gcc reload
gcc debug
gcc timeout list
```

## ❓ 故障排除

### 常见问题

**Q: AI没有响应？**
A: 请检查：
1. API密钥是否正确配置
2. 网络连接是否稳定
3. 是否处于冷却时间内
4. 查看控制台错误信息
5. 检查日志文件获取详细信息

**Q: 如何切换模型？**
A: 使用`/gcc model <模型名称>`命令。可用的模型名称可以在config.yml中查看。

**Q: 如何调整模型温度？**
A: 使用`/gcc temp <数值>`命令，数值范围为0.0-1.0。温度越高，回答越随机。

**Q: 经常出现超时错误？**
A: 
1. 增加配置中的读取超时时间
2. 启用长思考模式
3. 检查网络稳定性
4. 考虑使用更快的模型

**Q: 插件无法启动？**
A: 检查：
1. 已安装Java 21+
2. Minecraft版本为1.13+
3. API密钥配置正确
4. 没有冲突的插件

### 调试模式
启用调试模式以获得详细日志：
```bash
/gcc debug
```

这将显示：
- API请求详情
- 响应处理过程
- 超时信息
- 错误堆栈跟踪

## 🔗 API集成

### 支持的API
- **OpenAI**（GPT-3.5、GPT-4）
- **Anthropic**（Claude模型）
- **Google**（Gemini模型）
- **DeepSeek**（R1模型）
- **自定义API**（任何OpenAI兼容的API）

### 自定义API配置
```yaml
api5:
  name: "自定义API"
  model: "custom-model"
  base_url: "https://your-api.com/v1/chat/completions"
  api_key: "your-key"
  request:
    method: "POST"
    headers:
      "Content-Type": "application/json"
      "Authorization": "Bearer {api_key}"
    body_template: |
      {
        "model": "{model}",
        "messages": {messages},
        "temperature": {temperature}
      }
  response:
    content_path: "choices[0].message.content"
    error_path: "error.message"
```

## 🤝 贡献

我们欢迎贡献！请：

1. Fork仓库
2. 创建功能分支
3. 进行更改
4. 如适用，添加测试
5. 提交拉取请求

### 开发设置
1. 克隆仓库
2. 导入到IDE
3. 运行`mvn clean compile`构建
4. 使用本地Minecraft服务器测试

## 📄 许可证

本项目采用MIT许可证 - 详见[LICENSE](../../LICENSE)文件。

## 🆘 支持

- **GitHub Issues**：[报告错误或请求功能](https://github.com/ning-g-mo/gcc/issues)
- **QQ群**：603902151
- **文档**：[完整文档](../README.md)

---

**与AI愉快聊天！🤖✨**
