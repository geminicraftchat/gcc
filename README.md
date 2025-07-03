# GeminiCraftChat

[![版本](https://img.shields.io/badge/版本-1.0.7-blue.svg)](https://github.com/geminicraftchat/gcc/releases)
[![许可证](https://img.shields.io/badge/许可证-MIT-green.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.13+-orange.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-21+-red.svg)](https://openjdk.java.net/)

> **Language / 语言**: [English](docs/en/README.md) | [中文](docs/zh/README.md) | [📚 Documentation Hub](docs/README.md)

一个强大的Minecraft聊天AI插件，支持多种AI模型和高级功能。

## 🆕 最新更新 (v1.0.7)

- ✅ **5个全新可配置API接口** - 完全自定义的API集成
- ✅ **长思考功能** - 防止复杂AI推理时的超时问题
- ✅ **控制台命令支持** - 管理员可从控制台完全管理插件
- ✅ **全面日志系统** - API调用记录和详细统计信息
- ✅ **双语文档** - 完整的中英文文档支持
- ✅ **插件大小优化** - 从42MB优化至仅3.8MB
- ✅ **bStats统计** - 匿名使用统计，帮助改进插件开发

## 🌟 功能特点

- **多AI模型支持** - Gemini、Claude、GPT等主流AI模型
- **5个可配置API** - 完全自定义的API接口，支持任何兼容API
- **长思考模式** - 为复杂AI推理提供延长超时
- **控制台命令** - 从服务器控制台完全管理插件
- **自定义触发词** - 可配置的聊天触发器（支持中文）
- **人设系统** - 在不同AI人格之间切换
- **敏感词过滤** - 内置内容过滤系统
- **权限系统** - 完整的权限管理
- **详细日志** - 完整的活动跟踪和API调用记录
- **代理支持** - HTTP/SOCKS代理兼容

## 使用方法

### 聊天命令
- `ai <消息>` - 与AI对话
- `@ai <消息>` - 与AI对话（别名）
- `!ai <消息>` - 与AI对话（别名）

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
- `/gcc logs stats` - 查看统计信息
- `/gcc logs reset` - 重置统计信息

### 中文命令
- `清除记忆` - 清除你的对话历史
- `切换人设 <名称>` - 切换到指定人设
- `查看人设` - 显示所有可用人设
- `帮助` - 显示帮助信息

## 权限

- `gcc.use` - 允许使用基本功能
- `gcc.admin` - 允许使用管理员命令
- `gcc.model.switch` - 允许切换AI模型
- `gcc.temperature.adjust` - 允许调整模型温度
- `gcc.broadcast.receive` - 允许接收AI对话广播
- `gcc.broadcast.bypass` - 允许跳过AI对话广播

## 📈 bStats

[![bStats Graph Data](https://bstats.org/signatures/bukkit/GeminiCraftChat.svg)](https://bstats.org/plugin/bukkit/GeminiCraftChat/26354)

在我们的 [bStats页面](https://bstats.org/plugin/bukkit/GeminiCraftChat/26354) 查看详细的统计数据和图表

## 配置文件

### API设置
插件支持5个完全可配置的API接口：

```yaml
api:
  models:
    api1:
      name: "OpenAI GPT"
      model: "gpt-3.5-turbo"
      base_url: "https://api.openai.com/v1/chat/completions"
      api_key: "your-openai-key"
      max_tokens: 4096
      temperature: 0.7
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
      max_tokens: 4096
      temperature: 0.7
      timeout:
        connect: 30
        read: 180
        write: 30
        long_thinking: true

    api3:
      name: "Gemini Pro"
      model: "gemini-1.5-pro"
      base_url: "https://generativelanguage.googleapis.com/v1/chat/completions"
      api_key: "your-gemini-key"
      max_tokens: 4096
      temperature: 0.7
      timeout:
        connect: 30
        read: 120
        write: 30
        long_thinking: false

  current_model: "api1"
```

### 日志设置
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
    temperature_changes: true
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
- **思考模型**（Claude Opus、o1系列）：300-600秒读取超时

## 常见问题

**Q: 如何获取API密钥？**
A:
- OpenAI: 访问 [OpenAI API](https://platform.openai.com/api-keys) 创建API密钥
- Anthropic: 访问 [Anthropic Console](https://console.anthropic.com/) 创建API密钥
- Google: 访问 [Google AI Studio](https://makersuite.google.com/app/apikey) 创建API密钥

**Q: 为什么AI没有响应？**  
A: 请检查：
1. API密钥是否正确配置
2. 网络连接是否正常
3. 是否处于冷却时间内
4. 查看控制台错误信息
5. 检查日志文件获取详细信息

**Q: 如何切换模型？**  
A: 使用 `/gcc model <模型名称>` 命令。可用的模型名称可以在配置文件中查看。

**Q: 如何调整模型温度？**
A: 使用 `/gcc temp <数值>` 命令，数值范围为0.0-1.0。温度越高，回答越随机。

**Q: 经常出现超时错误？**
A:
1. 增加配置中的读取超时时间
2. 启用长思考模式：`/gcc timeout toggle <模型>`
3. 检查网络稳定性
4. 考虑使用更快的模型

## 🚀 快速开始

### 安装
1. 从[GitHub发布页面](https://github.com/geminicraftchat/gcc/releases)下载最新的`geminicraftchat-1.0.7.jar`
2. 将JAR文件放入服务器的`plugins`文件夹
3. 启动/重启服务器以生成配置文件
4. 在`plugins/GeminiCraftChat/config.yml`中配置API密钥
5. 使用`/gcc reload`重新加载配置
6. 使用`ai 你好！`开始聊天

### 系统要求
- **Minecraft**: 1.13+ (支持Bukkit、Spigot、Paper、Folia)
- **Java**: 21+
- **插件大小**: 仅3.8MB（高度优化）

## 🤝 贡献

我们欢迎贡献！请：

1. Fork仓库
2. 创建功能分支
3. 进行更改
4. 如适用，添加测试
5. 提交拉取请求

## 📄 许可证

本项目采用MIT许可证 - 详见[LICENSE](LICENSE)文件。

## 🆘 支持

如果遇到问题：
- **GitHub Issues**：[报告错误或请求功能](https://github.com/geminicraftchat/gcc/issues)
- **GitHub Releases**：[下载最新版本](https://github.com/geminicraftchat/gcc/releases)
- **完整文档**：[English](docs/en/README.md) | [中文](docs/zh/README.md)

---

**与AI愉快聊天！🤖✨**
