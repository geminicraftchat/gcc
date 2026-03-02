# GeminiCraftChat

[![版本](https://img.shields.io/badge/版本-1.0.9-blue.svg)](https://github.com/geminicraftchat/gcc/releases)
[![许可证](https://img.shields.io/badge/许可证-MIT-green.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.13+-orange.svg)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21.4-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21+-red.svg)](https://openjdk.java.net/)

> **Language / 语言**: [English](docs/en/README.md) | [中文](docs/zh/README.md) | [📚 Documentation Hub](docs/README.md)

一个极其强大且现代化的 Minecraft 聊天 AI 插件，现已完美适配 **Paper 1.21.4** 和 **Leaves** 服务端。支持多种大语言模型（LLM）集成。

## 🆕 最新更新 (v1.0.9)

- 🚀 **全面支持 Paper 1.21.4** - 采用最新的 `AsyncChatEvent` API。
- 🍃 **Leaves 深度优化** - 针对 Leaves 服务端的专有优化和 API 支持。
- ✨ **Adventure API 集成** - 全新消息渲染引擎，支持更丰富的交互体验。
- 🛡️ **线程安全增强** - 彻底修复多模型并发请求下的历史记录冲突问题。
- 📦 **依赖项升级** - 更新所有核心库至最新版本，确保最佳稳定性。

## 🌟 功能特点

- **多 AI 模型支持** - 完美适配 Gemini、Claude、GPT、DeepSeek、Qwen 等主流模型。
- **现代化架构** - 基于异步、非阻塞式请求设计，确保插件运行不会阻塞服务器主线程。
- **5 个可配置 API** - 极其灵活的 API 接口配置，支持自定义 Headers、Body 模板和响应路径。
- **长思考/推理模式** - 为 o1 系列、Claude Opus 等思考型模型提供可调节的超长超时。
- **人设 (Persona) 系统** - 预设多种 AI 人格，一键切换对话风格。
- **双语命令支持** - 同时支持传统的斜杠命令和更自然的中文聊天指令。
- **内容安全过滤** - 自定义敏感词库，并支持智能替换。
- **离线日志系统** - 详细记录所有 API 调用、错误和对话内容。
- **代理支持** - 完善的 HTTP/SOCKS5 代理兼容。

## 🛠️ 使用方法

### 聊天对话
- `ai <消息>` - 与当前 AI 进行对话。
- `@ai <消息>` 或 `!ai <消息>` - 快捷触发对话。

### 玩家常用命令
- `/gcc persona list` - 查看所有可用的人设列表。
- `/gcc persona switch <名称>` - 切换到指定人设。
- `/gcc clear` - 清除你的当前对话上下文历史。
- `/gcc model <名称>` - 在已配置的模型间快速切换。

### 中文快捷指令 (可配置)
- `帮助` - 显示插件帮助。
- `清除记忆` - 立即重置 AI 对你的记忆。
- `切换人设 <名称>` - 快速更换 AI 扮演的角色。
- `查看人设` - 列出所有已加载的对话模板。

### 管理员命令
- `/gcc reload` - 重新加载所有配置文件及 API 客户端。
- `/gcc debug` - 切换实时调试模式，在控制台查看 API 请求细节。
- `/gcc logs stats` - 查看全服 AI 使用量及响应时间统计。
- `/gcc timeout toggle <模型>` - 一键开启或关闭特定模型的长思考模式。

## ⚙️ 配置文件概览

### API 核心设置
您可以在 `config.yml` 中轻松定义私有 API 或第三方代理：

```yaml
api:
  models:
    api1:
      name: "GPT-4o"
      model: "gpt-4o"
      base_url: "https://api.openai.com/v1/chat/completions"
      api_key: "sk-..."
      timeout:
        connect: 30
        read: 120
        long_thinking: false
      request:
        body_template: '{"model":"{model}","messages":{messages},"temperature":{temperature}}'
      response:
        content_path: "choices[0].message.content"
```

## 🚀 快速开始

### 系统要求
- **服务端**: Paper / Leaves / Spigot 1.13+ (推荐使用最新版 Paper/Leaves)
- **Java**: 21 或更高版本
- **网络**: 需要可访问您所配置的 AI API 地址

### 安装步骤
1. [下载最新的 JAR 文件](https://github.com/geminicraftchat/gcc/releases)。
2. 放入 `plugins` 文件夹并启动服务器。
3. 编辑生成的 `plugins/GeminiCraftChat/config.yml`，填入您的 API Key。
4. 使用 `/gcc reload` 激活配置。
5. 在游戏内输入 `ai 你好! 你能做什么？` 开始探索！

## 🤝 贡献与反馈

如果您发现了问题或有好的建议，欢迎提交 **Issues** 或 **Pull Request**。

- **GitHub Issues**：[问题反馈](https://github.com/geminicraftchat/gcc/issues)
- **bStats**：[查看匿名使用统计](https://bstats.org/plugin/bukkit/GeminiCraftChat/26354)

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。
