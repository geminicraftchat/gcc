# GeminiCraftChat Long Thinking Feature Guide

## Overview

The Long Thinking feature allows AI models to spend more time on deep reasoning, particularly suitable for handling complex problems, creative writing, code analysis, and other tasks requiring deep reasoning. Each API interface can independently configure timeout settings and long thinking mode.

## Feature Highlights

### ✨ Key Features

1. **Independent Configuration**: Each API interface has independent timeout settings
2. **Flexible Switching**: Can dynamically enable/disable long thinking mode
3. **Smart Timeouts**: Configure different timeout periods based on model characteristics
4. **Real-time Management**: View and manage timeout settings through commands in real-time
5. **Logging**: All timeout-related operations are logged

### 🔧 Technical Implementation

- **Multi-client Architecture**: Create dedicated HTTP clients for each model
- **Dynamic Timeouts**: Dynamically adjust connection, read, and write timeouts based on configuration
- **Memory Optimization**: Client reuse and intelligent caching mechanisms
- **Graceful Shutdown**: Ensure all clients are properly closed

## Configuration Guide

### Basic Configuration Structure

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
      # Timeout settings (seconds)
      timeout:
        connect: 30           # Connection timeout
        read: 60             # Read timeout
        write: 30            # Write timeout
        long_thinking: false # Long thinking mode switch
```

### Timeout Parameters Explained

#### connect (Connection Timeout)
- **Purpose**: Maximum wait time to establish HTTP connection
- **Recommended**: 30 seconds
- **Range**: 10-60 seconds
- **Note**: Too short may cause connection failures; too long may block other requests

#### read (Read Timeout)
- **Purpose**: Maximum wait time to receive API response
- **Recommended**: 60-300 seconds (depending on model)
- **Range**: 30-600 seconds
- **Note**: This is the most important setting for long thinking

#### write (Write Timeout)
- **Purpose**: Maximum time to send request data
- **Recommended**: 30 seconds
- **Range**: 10-60 seconds
- **Note**: Usually doesn't need adjustment unless network is very slow

#### long_thinking (Long Thinking Mode)
- **Purpose**: Enable/disable extended thinking mode
- **Values**: true/false
- **Effect**: When enabled, automatically uses longer read timeout
- **Default**: false

## Model-Specific Recommendations

### Fast Models (GPT-3.5, Basic APIs)
```yaml
timeout:
  connect: 30
  read: 60
  write: 30
  long_thinking: false
```
**Use Case**: Quick responses, simple Q&A, basic conversations

### Balanced Models (GPT-4, Claude Haiku)
```yaml
timeout:
  connect: 30
  read: 120
  write: 30
  long_thinking: true
```
**Use Case**: Moderate complexity tasks, detailed explanations, code review

### Thinking Models (Claude Opus, DeepSeek R1)
```yaml
timeout:
  connect: 30
  read: 300
  write: 30
  long_thinking: true
```
**Use Case**: Complex reasoning, creative writing, deep analysis

### Research Models (o1-preview, o1-mini)
```yaml
timeout:
  connect: 30
  read: 600
  write: 30
  long_thinking: true
```
**Use Case**: Research tasks, complex problem solving, academic analysis

## Command Usage

### View All Model Timeouts
```bash
/gcc timeout list
```
**Output Example**:
```
Model Timeout Settings:
api1 (GPT-3.5 Turbo):
  Connect: 30s | Read: 60s | Write: 30s | Long Thinking: OFF
api2 (Claude Sonnet):
  Connect: 30s | Read: 180s | Write: 30s | Long Thinking: ON
api3 (DeepSeek R1):
  Connect: 30s | Read: 300s | Write: 30s | Long Thinking: ON
```

### View Specific Model Timeout Details
```bash
/gcc timeout info api2
```
**Output Example**:
```
Model: api2 (Claude Sonnet)
Current Timeout Settings:
  Connection Timeout: 30 seconds
  Read Timeout: 180 seconds
  Write Timeout: 30 seconds
  Long Thinking Mode: ENABLED
  
Last Modified: 2024-01-15 14:30:25
Status: Active
Recent Performance:
  Average Response Time: 45.2s
  Success Rate: 98.5%
  Timeout Errors: 2 (last 24h)
```

### Toggle Long Thinking Mode
```bash
/gcc timeout toggle api2
```
**Output Example**:
```
✅ Long thinking mode for api2 (Claude Sonnet) has been ENABLED
New read timeout: 180 seconds
```

### Set Custom Timeout (Advanced)
```bash
/gcc timeout set api2 read 300
```
**Output Example**:
```
✅ Read timeout for api2 (Claude Sonnet) set to 300 seconds
Long thinking mode: ENABLED
```

## Usage Scenarios

### 1. Creative Writing
**Recommended Settings**:
```yaml
timeout:
  read: 240
  long_thinking: true
```
**Models**: Claude Opus, GPT-4, DeepSeek R1
**Commands**: 
- `/gcc timeout toggle claude_opus`
- `/gcc model claude_opus`

### 2. Code Analysis
**Recommended Settings**:
```yaml
timeout:
  read: 180
  long_thinking: true
```
**Models**: GPT-4, Claude Sonnet, DeepSeek R1
**Commands**:
- `/gcc timeout set api2 read 180`
- `/gcc model api2`

### 3. Complex Problem Solving
**Recommended Settings**:
```yaml
timeout:
  read: 600
  long_thinking: true
```
**Models**: o1-preview, o1-mini, Claude Opus
**Commands**:
- `/gcc timeout set o1_preview read 600`
- `/gcc model o1_preview`

### 4. Quick Q&A
**Recommended Settings**:
```yaml
timeout:
  read: 60
  long_thinking: false
```
**Models**: GPT-3.5, Claude Haiku
**Commands**:
- `/gcc timeout toggle gpt35 # to disable`
- `/gcc model gpt35`

## Performance Monitoring

### Real-time Monitoring
```bash
# Check current performance
/gcc logs stats

# View recent timeouts
/gcc logs errors

# Monitor specific model
/gcc timeout info api2
```

### Performance Metrics
The system tracks:
- **Average Response Time**: Per model response times
- **Timeout Rate**: Percentage of requests that timeout
- **Success Rate**: Overall API call success rate
- **Peak Response Time**: Longest response time recorded

### Optimization Tips

#### If Experiencing Frequent Timeouts:
1. **Increase read timeout**: `/gcc timeout set <model> read <seconds>`
2. **Enable long thinking**: `/gcc timeout toggle <model>`
3. **Switch to faster model**: `/gcc model <faster_model>`
4. **Check network connectivity**: `/gcc test <model>`

#### If Responses Are Too Slow:
1. **Decrease read timeout**: `/gcc timeout set <model> read <seconds>`
2. **Disable long thinking**: `/gcc timeout toggle <model>`
3. **Switch to faster model**: `/gcc model <faster_model>`
4. **Adjust temperature**: `/gcc temp 0.3` (lower = faster)

## Best Practices

### Configuration
1. **Start Conservative**: Begin with shorter timeouts and increase as needed
2. **Model-Specific**: Configure timeouts based on model characteristics
3. **Monitor Performance**: Regularly check timeout statistics
4. **Test Changes**: Test timeout changes with actual usage

### Usage
1. **Match Task Complexity**: Use longer timeouts for complex tasks
2. **Consider User Experience**: Balance response quality vs. wait time
3. **Peak Hours**: Consider shorter timeouts during high-traffic periods
4. **Fallback Models**: Have faster models as backup options

### Troubleshooting
1. **Enable Debug Mode**: `/gcc debug` for detailed timeout information
2. **Check Logs**: Review error logs for timeout patterns
3. **Test Connectivity**: Use `/gcc test <model>` to verify API access
4. **Monitor Resources**: Check server memory and CPU usage

## Advanced Configuration

### Dynamic Timeout Adjustment
```yaml
# Example: Automatic timeout scaling based on server load
timeout:
  connect: 30
  read: 120
  write: 30
  long_thinking: true
  # Advanced settings
  auto_scale: true          # Enable automatic scaling
  scale_factor: 1.5         # Multiply timeout by this factor when enabled
  max_timeout: 600          # Maximum allowed timeout
  min_timeout: 30           # Minimum allowed timeout
```

### Load Balancing with Timeouts
```yaml
# Example: Different timeout strategies for load balancing
api1:  # Fast model for quick responses
  timeout:
    read: 60
    long_thinking: false
    
api2:  # Balanced model for moderate complexity
  timeout:
    read: 180
    long_thinking: true
    
api3:  # Slow model for complex tasks
  timeout:
    read: 600
    long_thinking: true
```

## Troubleshooting

### Common Issues

**Q: Model keeps timing out?**
A: 
1. Increase read timeout: `/gcc timeout set <model> read 300`
2. Enable long thinking: `/gcc timeout toggle <model>`
3. Check API status and network connectivity
4. Consider switching to a faster model

**Q: Responses are too slow?**
A:
1. Decrease read timeout: `/gcc timeout set <model> read 60`
2. Disable long thinking: `/gcc timeout toggle <model>`
3. Switch to a faster model: `/gcc model <faster_model>`
4. Lower temperature: `/gcc temp 0.3`

**Q: Timeout commands not working?**
A:
1. Ensure you have admin permissions
2. Check if the model name is correct: `/gcc models`
3. Verify the plugin is updated to the latest version
4. Check console for error messages

**Q: Long thinking mode not taking effect?**
A:
1. Reload the plugin: `/gcc reload`
2. Check configuration syntax in config.yml
3. Verify the model is properly configured
4. Enable debug mode: `/gcc debug`

### Debug Information

Enable debug mode for detailed timeout information:
```bash
/gcc debug
```

This will show:
- Detailed timeout configuration loading
- HTTP client creation and configuration
- Request/response timing information
- Timeout error details and stack traces

---

**Think deeper, achieve more! 🧠⚡**
