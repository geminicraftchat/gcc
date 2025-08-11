# 📦 GeminiCraftChat v1.0.8 Installation Guide

## 🔧 Installation Steps

1. **Download JAR File**
   - Download `geminicraftchat-1.0.8.jar` from this release

2. **Install Plugin**
   - Place the JAR file in your server's `plugins` folder
   - Restart your server to generate configuration files

3. **Configure API**
   - Edit `plugins/GeminiCraftChat/config.yml`
   - Add your AI API keys and configure endpoints

4. **Reload Configuration**
   - Use `/gcc reload` to reload the configuration

5. **Start Chatting**
   - Use `ai Hello!` to start chatting with AI
   - Right-click NPCs to interact with them

## ⚙️ Quick Configuration

### Basic AI Setup
```yaml
apis:
  api1:
    enabled: true
    model_name: "gemini-1.5-pro"
    api_key: "YOUR_API_KEY_HERE"
    base_url: "https://generativelanguage.googleapis.com/v1beta/models/"
```

### Enable NPC System
```yaml
npc:
  enabled: true
  smart_npc_enabled: true
```

## 🎮 First Steps

1. **Test AI Chat**: `ai How are you?`
2. **List NPCs**: `/gcc npc list`
3. **Find Nearby NPCs**: `/gcc npc nearby`
4. **Chat with NPC**: Right-click on NPC or use `/gcc npc chat <ID> <message>`

## 📋 Requirements

- **Minecraft**: 1.13 or higher
- **Java**: 21 or higher
- **Server Software**: Bukkit, Spigot, or Paper
- **Memory**: At least 1GB RAM recommended
- **AI API**: Valid API key for supported AI services

## 🆘 Troubleshooting

### Common Issues
- **Plugin not loading**: Check Java version (requires Java 21+)
- **AI not responding**: Verify API key and network connectivity
- **NPCs not spawning**: Check world name in NPC configuration
- **Performance issues**: Adjust performance settings in config

### Getting Help
- Check the [GitHub Issues](https://github.com/geminicraftchat/gcc/issues)
- Read the [full documentation](https://github.com/geminicraftchat/gcc/blob/main/README.md)
- Join our community discussions

---

**Happy Gaming with AI-Powered NPCs!** 🎮🤖
