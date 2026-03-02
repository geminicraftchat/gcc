# GeminiCraftChat Console Commands Guide

## Overview

GeminiCraftChat plugin now fully supports console command execution. Administrators can use all plugin commands from the server console without entering the game.

## Console Permissions

- **Console has all admin permissions by default**
- No additional permission configuration needed
- Console can execute all commands
- Player-specific functions will show appropriate messages

## Available Commands

### Basic Management Commands

#### Reload Configuration
```
gcc reload
```
- Reload plugin configuration file
- Apply new settings without server restart

#### Clear Chat History
```
gcc clear all
```
- Clear all players' chat history
- Console cannot clear individual player history

#### Toggle Debug Mode
```
gcc debug
```
- Enable/disable debug mode
- Shows detailed logging information

### Model Management Commands

#### List Available Models
```
gcc model list
gcc models
```
- Display all configured API models
- Shows model names and display names

#### Switch Current Model
```
gcc model switch <model_name>
gcc model <model_name>
```
- Switch to specified model
- Example: `gcc model api2`

#### Show Current Model
```
gcc model info
gcc model current
```
- Display currently active model
- Shows model configuration details

#### Show Model Details
```
gcc model info <model_name>
```
- Display detailed information about specific model
- Shows API configuration, timeout settings, etc.

### Temperature Management

#### Show Current Temperature
```
gcc temp
gcc temperature
```
- Display current model temperature setting

#### Set Temperature
```
gcc temp <value>
gcc temperature <value>
```
- Set model temperature (0.0-1.0)
- Example: `gcc temp 0.8`

### Persona Management Commands

#### List All Personas
```
gcc persona list
```
- Display all available personas
- Shows persona names and descriptions

#### Show Persona Details
```
gcc persona info <persona_name>
```
- Display detailed persona information
- Shows system prompt and configuration

#### Set Player Persona (Admin Only)
```
gcc persona set <player_name> <persona_name>
```
- Set specific persona for a player
- Example: `gcc persona set Steve teacher`

### Timeout Management Commands (Long Thinking Feature)

#### List All Model Timeouts
```
gcc timeout list
```
- Display timeout settings for all models
- Shows connect, read, write timeouts and long thinking status

#### Show Model Timeout Details
```
gcc timeout info <model_name>
```
- Display detailed timeout information for specific model
- Example: `gcc timeout info api2`

#### Toggle Long Thinking Mode
```
gcc timeout toggle <model_name>
```
- Enable/disable long thinking mode for specific model
- Example: `gcc timeout toggle api2`

#### Set Custom Timeout (Advanced)
```
gcc timeout set <model_name> <type> <seconds>
```
- Set specific timeout value
- Types: connect, read, write
- Example: `gcc timeout set api2 read 300`

### Logging Commands

#### View Statistics
```
gcc logs stats
gcc statistics
```
- Display usage statistics
- Shows API call counts, response times, etc.

#### Reset Statistics
```
gcc logs reset
```
- Reset all statistics counters
- Clears accumulated data

#### Export Player Statistics
```
gcc logs export
```
- Export player usage statistics to file
- Creates CSV file in logs directory

#### View Recent Errors
```
gcc logs errors
```
- Display recent error messages
- Shows last 10 errors with timestamps

### Player Management Commands

#### Clear Specific Player History
```
gcc clear player <player_name>
```
- Clear chat history for specific player
- Example: `gcc clear player Steve`

#### Show Player Information
```
gcc player info <player_name>
```
- Display player's current settings
- Shows persona, chat history count, etc.

#### List Active Players
```
gcc players
gcc player list
```
- Display all players who have used the AI chat
- Shows last activity time

### System Information Commands

#### Show Plugin Status
```
gcc status
gcc info
```
- Display plugin status and information
- Shows version, loaded models, active connections

#### Show Configuration Summary
```
gcc config
```
- Display current configuration summary
- Shows key settings and values

#### Test API Connection
```
gcc test <model_name>
gcc test
```
- Test API connection for specific model (or current model)
- Sends test request to verify connectivity

### Advanced Commands

#### Force Garbage Collection
```
gcc gc
```
- Force Java garbage collection
- Helps free up memory

#### Show Memory Usage
```
gcc memory
```
- Display current memory usage
- Shows heap usage and available memory

#### Backup Configuration
```
gcc backup
```
- Create backup of current configuration
- Saves to backups directory with timestamp

## Command Examples

### Daily Administration
```bash
# Check plugin status
gcc status

# View current model and settings
gcc model info
gcc temp

# Check recent activity
gcc logs stats
gcc players

# Test API connectivity
gcc test
```

### Model Management
```bash
# List all available models
gcc models

# Switch to different model
gcc model api2

# Adjust temperature for better responses
gcc temp 0.7

# Check timeout settings
gcc timeout list
```

### Long Thinking Management
```bash
# View all timeout settings
gcc timeout list

# Enable long thinking for complex model
gcc timeout toggle api2

# Check specific model timeout details
gcc timeout info api2

# Set custom timeout for research tasks
gcc timeout set api2 read 600
```

### Troubleshooting
```bash
# Enable debug mode
gcc debug

# Check recent errors
gcc logs errors

# Test API connection
gcc test api1

# Reload configuration after changes
gcc reload
```

### Player Support
```bash
# Help player with persona issues
gcc persona list
gcc persona set PlayerName teacher

# Clear player's stuck chat history
gcc clear player PlayerName

# Check player's current settings
gcc player info PlayerName
```

## Output Examples

### Model List Output
```
Available Models:
- api1: OpenAI GPT-3.5 (Current)
- api2: Claude Sonnet
- api3: Google Gemini
- api4: DeepSeek R1
- api5: Custom API
```

### Timeout List Output
```
Model Timeout Settings:
api1 (OpenAI GPT-3.5):
  Connect: 30s | Read: 60s | Write: 30s | Long Thinking: OFF
api2 (Claude Sonnet):
  Connect: 30s | Read: 180s | Write: 30s | Long Thinking: ON
api3 (Google Gemini):
  Connect: 30s | Read: 90s | Write: 30s | Long Thinking: ON
```

### Statistics Output
```
GeminiCraftChat Statistics:
Total API Calls: 1,234
Successful Calls: 1,198 (97.1%)
Failed Calls: 36 (2.9%)
Average Response Time: 2.3s
Most Used Model: api1 (45.2%)
Active Players: 23
```

## Best Practices

### Regular Maintenance
1. **Check statistics daily**: `gcc logs stats`
2. **Monitor errors**: `gcc logs errors`
3. **Test API connectivity**: `gcc test`
4. **Backup configuration**: `gcc backup`

### Performance Optimization
1. **Monitor memory usage**: `gcc memory`
2. **Adjust timeouts based on usage**: `gcc timeout list`
3. **Clear old statistics**: `gcc logs reset` (monthly)

### Player Support
1. **Check player issues**: `gcc player info <name>`
2. **Reset stuck conversations**: `gcc clear player <name>`
3. **Adjust personas for users**: `gcc persona set <name> <persona>`

### Model Management
1. **Switch models based on load**: `gcc model <name>`
2. **Adjust temperature for quality**: `gcc temp <value>`
3. **Enable long thinking for complex tasks**: `gcc timeout toggle <model>`

## Troubleshooting

### Common Issues

**Q: Commands not working from console?**
A: Ensure you're using the exact command syntax without the `/` prefix.

**Q: Cannot switch models?**
A: Check if the model is properly configured in config.yml and reload the plugin.

**Q: Timeout commands not available?**
A: Update to the latest version that includes the Long Thinking feature.

**Q: Statistics not showing?**
A: Enable logging in config.yml and restart the plugin.

### Debug Information

Enable debug mode to see detailed command execution:
```bash
gcc debug
```

This will show:
- Command processing details
- API call information
- Configuration loading status
- Error stack traces

## Security Notes

- **Console Access**: Only trusted administrators should have console access
- **API Keys**: Never display API keys in console output
- **Player Data**: Be careful when managing player-specific data
- **Backup**: Regular configuration backups are recommended

---

**Efficient server management! 🖥️⚡**
