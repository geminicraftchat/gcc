# GeminiCraftChat

GeminiCraftChat 是一个 Minecraft 插件，它将 Google Gemini AI 带入您的服务器，为玩家提供智能对话体验。

## 特性

- 🤖 与 Google Gemini AI 进行自然对话
- 🎭 多种预设人设（游戏专家、RPG角色等）
- 💬 支持中文命令和聊天
- 🔄 独立的对话历史记录
- ⚡ 异步处理，不影响服务器性能
- 🛡️ 内置敏感词过滤
- 🎨 可自定义的消息格式
- ⚙️ 高度可配置

## 安装

1. 从 [Releases](https://github.com/yourusername/geminicraftchat/releases) 下载最新版本的插件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 重启服务器或重载插件
4. 配置 `plugins/GeminiCraftChat/config.yml` 中的 API 密钥

## 配置

在 `config.yml` 中设置：

```yaml
api:
  key: "your-api-key-here" # 你的 Gemini API 密钥
  model: "gemini-pro" # 使用的模型
```


## 使用方法

### 聊天命令
- `ai <消息>` - 与 AI 对话
- `@ai <消息>` - 与 AI 对话（别名）
- `!ai <消息>` - 与 AI 对话（别名）

### 中文命令
- `清除记忆` - 清除你的对话历史
- `切换人设 <名称>` - 切换到指定人设
- `查看人设` - 显示所有可用人设
- `帮助` - 显示帮助信息

### 管理员命令
- `/gcc reload` - 重新加载配置
- `/gcc clear all` - 清除所有玩家的对话历史
- `/gcc debug` - 切换调试模式

## 权限

- `gcc.use` - 允许使用基本功能
- `gcc.admin` - 允许使用管理员命令

## 人设系统

预设人设包括：
- 默认助手：友好的 AI 助手
- MC专家：Minecraft 游戏专家
- RPG角色：角色扮演游戏中的 NPC

## 自定义配置

您可以在 `config.yml` 中自定义：
- 触发词
- 消息格式
- 冷却时间
- 历史记录长度
- 代理设置
- 敏感词过滤
- 更多人设

## 常见问题

**Q: 如何获取 API 密钥？**  
A: 访问 [Google AI Studio](https://makersuite.google.com/app/apikey) 创建 API 密钥。

**Q: 为什么 AI 没有响应？**  
A: 请检查：
1. API 密钥是否正确配置
2. 网络连接是否正常
3. 是否处于冷却时间内
4. 查看控制台错误信息

## 开发计划

- [ ] 支持图片识别
- [ ] 多语言支持
- [ ] Web 管理界面
- [ ] 更多预设人设
- [ ] 对话记录导出

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

本项目采用 MIT 许可证。

## 支持

如果遇到问题：
提交 [Issue](https://github.com/ning-g-mo/gcc/issues)
群聊：603902151


## 更新日志

### v1.0.0
- 初始发布
- 基本对话功能
- 人设系统
- 中文命令支持