# GeminiCraftChat

A powerful Minecraft AI chat plugin supporting multiple AI models with advanced features.

## 🌟 Features

### Core Features
- **Multi-AI Model Support** - Gemini, Claude, GPT, DeepSeek, and more
- **Custom Trigger Words** - Configurable chat triggers
- **Persona System** - Switch between different AI personalities
- **Word Filtering** - Built-in content filtering system
- **Permission System** - Comprehensive permission management
- **Detailed Logging** - Complete activity tracking

### Advanced Features
- **Long Thinking Mode** - Extended timeout for complex AI reasoning
- **Console Commands** - Full admin control from server console
- **5 Configurable APIs** - Completely customizable API interfaces
- **Real-time Switching** - Change models and settings on-the-fly
- **Chat History** - Persistent conversation memory
- **Proxy Support** - HTTP/SOCKS proxy compatibility
- **bStats Analytics** - Anonymous usage statistics to improve plugin development

## 🚀 Quick Start

### Installation
1. Download the latest `geminicraftchat-x.x.x.jar` from releases
2. Place the JAR file in your server's `plugins` folder
3. Start/restart your server to generate the config file
4. Configure your API keys in `plugins/GeminiCraftChat/config.yml`
5. Use `/gcc reload` to reload the configuration
6. Start chatting with `ai Hello!`

### Basic Configuration
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

## 💬 Usage

### Chat Commands
- `ai <message>` - Chat with AI
- `@ai <message>` - Alternative trigger
- `!ai <message>` - Alternative trigger

### Basic Commands
- `/gcc model <name>` - Switch AI model
- `/gcc temp <value>` - Adjust model temperature (0.0-1.0)
- `/gcc persona list` - Show available personas
- `/gcc persona switch <name>` - Switch to specific persona
- `/gcc clear` - Clear your chat history

### Admin Commands
- `/gcc reload` - Reload configuration
- `/gcc clear all` - Clear all players' chat history
- `/gcc debug` - Toggle debug mode
- `/gcc timeout list` - View timeout settings for all models
- `/gcc timeout info <model>` - View detailed timeout info for a model
- `/gcc timeout toggle <model>` - Toggle long thinking mode for a model

### Natural Language Commands
- `clear memory` - Clear your chat history
- `switch persona <name>` - Switch to specific persona
- `show personas` - Display available personas
- `help` - Show help information

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `gcc.use` | Allow basic functionality | `true` |
| `gcc.admin` | Allow admin commands | `op` |
| `gcc.model.switch` | Allow switching AI models | `op` |
| `gcc.temperature.adjust` | Allow adjusting model temperature | `op` |
| `gcc.broadcast.receive` | Allow receiving AI chat broadcasts | `true` |
| `gcc.broadcast.bypass` | Allow bypassing AI chat broadcasts | `op` |

## ⚙️ Configuration

### API Models
The plugin supports 5 completely configurable API interfaces:

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

### Long Thinking Feature
Configure different timeout settings for each model:

- **connect**: Connection timeout (seconds)
- **read**: Response timeout (seconds)
- **write**: Write timeout (seconds)
- **long_thinking**: Enable extended thinking mode

Recommended timeout settings:
- **Fast models** (GPT-3.5): 60s read timeout
- **Balanced models** (GPT-4, Claude): 120-180s read timeout
- **Thinking models** (Claude Opus, DeepSeek R1): 300-600s read timeout

### Chat Settings
```yaml
chat:
  trigger: "ai"
  trigger_words: ["@ai", "!ai"]
  max_history: 10
  cooldown: 10000
  format:
    thinking: "§7[AI] §fThinking..."
    response: "§7[AI] §f%s"
    error: "§c[AI] Error: %s"
```

### Logging Configuration
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

### bStats Configuration
```yaml
# bStats Analytics Settings
# bStats collects anonymous usage statistics to help improve the plugin
# Visit: https://bstats.org/plugin/bukkit/GeminiCraftChat/26354
bstats:
  enabled: true  # Enable bStats analytics (recommended to support plugin development)
```

**What data is collected?**
- Server software type (Paper, Spigot, etc.)
- Java version
- Number of configured APIs
- Enabled features (long thinking, proxy, filtering)
- Anonymous usage statistics

**Privacy**: All data is completely anonymous and helps improve the plugin.

## 📈 bStats

[![bStats Graph Data](https://bstats.org/signatures/bukkit/GeminiCraftChat.svg)](https://bstats.org/plugin/bukkit/GeminiCraftChat/26354)

View detailed statistics and charts on our [bStats page](https://bstats.org/plugin/bukkit/GeminiCraftChat/26354)

## 🧠 Long Thinking Mode

The Long Thinking feature allows AI models to take more time for complex reasoning:

### Benefits
- **Better Quality**: More thoughtful and detailed responses
- **Complex Problems**: Handles mathematical proofs, code analysis, creative writing
- **Flexible Configuration**: Per-model timeout settings
- **Real-time Management**: Toggle on/off without restart

### Usage
```bash
# View all model timeout settings
/gcc timeout list

# View specific model details
/gcc timeout info api2

# Toggle long thinking mode
/gcc timeout toggle api2
```

### Best Practices
1. **Use for complex tasks**: Enable for models handling difficult problems
2. **Monitor performance**: Check response times and adjust accordingly
3. **Balance quality vs speed**: Choose appropriate timeout values
4. **Network considerations**: Account for network latency

## 🎭 Persona System

Create different AI personalities:

```yaml
personas:
  default:
    name: "Assistant"
    description: "A helpful AI assistant"
    system_prompt: "You are a helpful assistant."
  
  teacher:
    name: "Teacher"
    description: "An educational AI teacher"
    system_prompt: "You are a knowledgeable teacher who explains concepts clearly."
  
  friend:
    name: "Friend"
    description: "A casual, friendly AI"
    system_prompt: "You are a casual, friendly companion."
```

## 🛡️ Word Filtering

Configure content filtering:

```yaml
filter:
  enabled: true
  words:
    - "badword1"
    - "badword2"
  replacement: "***"
```

## 🌐 Proxy Support

Configure HTTP/SOCKS proxy:

```yaml
api:
  http_proxy:
    enabled: true
    host: "127.0.0.1"
    port: 7890
    type: "SOCKS"  # or "HTTP"
```

## 📊 Logging System

The plugin provides comprehensive logging:

### Log Categories
- **Chat Logs**: All AI conversations
- **Command Logs**: Command usage tracking
- **API Logs**: API request/response details
- **Error Logs**: Error tracking and debugging
- **Statistics**: Usage statistics and performance metrics

### Log Commands
```bash
/gcc logs stats    # View statistics
/gcc logs reset    # Reset statistics
/gcc logs export   # Export player statistics
```

## 🔧 Console Commands

Full admin control from server console:

```bash
# Model management
gcc model list
gcc model switch api2
gcc model info api1

# Player management
gcc clear player PlayerName
gcc persona set PlayerName teacher

# System management
gcc reload
gcc debug
gcc timeout list
```

## ❓ Troubleshooting

### Common Issues

**Q: AI doesn't respond?**
A: Check:
1. API key is correctly configured
2. Network connection is stable
3. Not in cooldown period
4. Check console for error messages
5. Review log files for details

**Q: How to switch models?**
A: Use `/gcc model <model_name>` command. Available models are shown in config.yml.

**Q: How to adjust model temperature?**
A: Use `/gcc temp <value>` command, where value is between 0.0-1.0. Higher values make responses more random.

**Q: Timeout errors frequently?**
A: 
1. Increase read timeout in config
2. Enable long thinking mode
3. Check network stability
4. Consider using a faster model

**Q: Plugin won't start?**
A: Check:
1. Java 21+ is installed
2. Minecraft version is 1.13+
3. API keys are properly configured
4. No conflicting plugins

### Debug Mode
Enable debug mode for detailed logging:
```bash
/gcc debug
```

This will show:
- API request details
- Response processing
- Timeout information
- Error stack traces

## 🔗 API Integration

### Supported APIs
- **OpenAI** (GPT-3.5, GPT-4)
- **Anthropic** (Claude models)
- **Google** (Gemini models)
- **DeepSeek** (R1 models)
- **Custom APIs** (Any OpenAI-compatible API)

### Custom API Configuration
```yaml
api5:
  name: "Custom API"
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

## 🤝 Contributing

We welcome contributions! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

### Development Setup
1. Clone the repository
2. Import into your IDE
3. Run `mvn clean compile` to build
4. Test with a local Minecraft server

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## 🆘 Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/ning-g-mo/gcc/issues)
- **Discord**: Coming soon
- **Documentation**: [Full documentation](../README.md)

---

**Happy chatting with AI! 🤖✨**
