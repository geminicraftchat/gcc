# GeminiCraftChat API Configuration Guide

## Overview

GeminiCraftChat now supports fully configurable API interfaces. You can customize any AI service provider's API calling method in the configuration file without modifying the code.

## Configuration Structure

Each API interface configuration includes the following parts:

```yaml
api:
  models:
    your_api_name:  # Custom API name
      name: "Display Name"
      model: "model-name"
      base_url: "API URL"
      api_key: "API Key"
      max_tokens: 4096
      temperature: 0.7
      
      # Timeout configuration (Long Thinking Feature)
      timeout:
        connect: 30        # Connection timeout (seconds)
        read: 60          # Read timeout (seconds)
        write: 30         # Write timeout (seconds)
        long_thinking: false  # Enable long thinking mode
      
      # Request configuration
      request:
        method: "POST"  # Request method
        headers:        # Request headers
          "Content-Type": "application/json"
          "Authorization": "Bearer {api_key}"
        body_template: |  # Request body template
          {
            "model": "{model}",
            "messages": {messages},
            "temperature": {temperature},
            "max_tokens": {max_tokens}
          }
      
      # Response parsing configuration
      response:
        content_path: "choices[0].message.content"  # Response content path
        error_path: "error.message"                 # Error message path
      
      # Additional parameters
      parameters:
        frequency_penalty: 0.0
        presence_penalty: 0.0
```

## Variable Substitution

The following variables can be used in the configuration:

### Basic Variables
- `{api_key}` - API key
- `{model}` - Model name
- `{temperature}` - Temperature value
- `{max_tokens}` - Maximum tokens
- `{messages}` - Chat messages (JSON array)

### Message Variables
- `{user_message}` - Current user message
- `{system_prompt}` - System prompt (persona)
- `{history}` - Chat history

### Custom Variables
You can add custom variables in the `parameters` section and reference them using `{parameter_name}`.

## Response Path Syntax

Use JSON path syntax to extract content from API responses:

### Simple Path
```yaml
content_path: "content"  # response.content
```

### Nested Path
```yaml
content_path: "choices[0].message.content"  # response.choices[0].message.content
```

### Array Index
```yaml
content_path: "data[0].text"  # response.data[0].text
```

## API Examples

### 1. OpenAI GPT
```yaml
api1:
  name: "OpenAI GPT-3.5"
  model: "gpt-3.5-turbo"
  base_url: "https://api.openai.com/v1/chat/completions"
  api_key: "sk-your-openai-key"
  max_tokens: 4096
  temperature: 0.7
  timeout:
    connect: 30
    read: 60
    write: 30
    long_thinking: false
  request:
    method: "POST"
    headers:
      "Content-Type": "application/json"
      "Authorization": "Bearer {api_key}"
    body_template: |
      {
        "model": "{model}",
        "messages": {messages},
        "temperature": {temperature},
        "max_tokens": {max_tokens}
      }
  response:
    content_path: "choices[0].message.content"
    error_path: "error.message"
```

### 2. Anthropic Claude
```yaml
api2:
  name: "Claude Sonnet"
  model: "claude-3-sonnet-20240229"
  base_url: "https://api.anthropic.com/v1/messages"
  api_key: "sk-ant-your-anthropic-key"
  max_tokens: 4096
  temperature: 0.7
  timeout:
    connect: 30
    read: 180
    write: 30
    long_thinking: true  # Claude benefits from longer thinking time
  request:
    method: "POST"
    headers:
      "Content-Type": "application/json"
      "x-api-key": "{api_key}"
      "anthropic-version": "2023-06-01"
    body_template: |
      {
        "model": "{model}",
        "messages": {messages},
        "max_tokens": {max_tokens},
        "temperature": {temperature}
      }
  response:
    content_path: "content[0].text"
    error_path: "error.message"
  parameters:
    top_p: 1.0
    top_k: 40
```

### 3. Google Gemini
```yaml
api3:
  name: "Google Gemini"
  model: "gemini-pro"
  base_url: "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
  api_key: "your-gemini-key"
  max_tokens: 4096
  temperature: 0.7
  timeout:
    connect: 30
    read: 90
    write: 30
    long_thinking: true
  request:
    method: "POST"
    headers:
      "Content-Type": "application/json"
    body_template: |
      {
        "contents": {messages},
        "generationConfig": {
          "temperature": {temperature},
          "maxOutputTokens": {max_tokens}
        }
      }
  response:
    content_path: "candidates[0].content.parts[0].text"
    error_path: "error.message"
```

### 4. DeepSeek R1
```yaml
api4:
  name: "DeepSeek R1"
  model: "deepseek-r1"
  base_url: "https://api.deepseek.com/v1/chat/completions"
  api_key: "sk-your-deepseek-key"
  max_tokens: 4096
  temperature: 0.7
  timeout:
    connect: 30
    read: 300  # R1 needs more time for reasoning
    write: 30
    long_thinking: true
  request:
    method: "POST"
    headers:
      "Content-Type": "application/json"
      "Authorization": "Bearer {api_key}"
    body_template: |
      {
        "model": "{model}",
        "messages": {messages},
        "temperature": {temperature},
        "max_tokens": {max_tokens}
      }
  response:
    content_path: "choices[0].message.content"
    error_path: "error.message"
```

### 5. Custom API
```yaml
api5:
  name: "Custom API"
  model: "custom-model"
  base_url: "https://your-api.com/v1/chat"
  api_key: "your-custom-key"
  max_tokens: 4096
  temperature: 0.7
  timeout:
    connect: 30
    read: 120
    write: 30
    long_thinking: false
  request:
    method: "POST"
    headers:
      "Content-Type": "application/json"
      "X-API-Key": "{api_key}"
      "User-Agent": "GeminiCraftChat/1.0"
    body_template: |
      {
        "model": "{model}",
        "prompt": "{user_message}",
        "temperature": {temperature},
        "max_length": {max_tokens},
        "custom_param": "{custom_value}"
      }
  response:
    content_path: "response.text"
    error_path: "error.details"
  parameters:
    custom_value: "example"
    top_p: 0.9
```

## Long Thinking Configuration

The Long Thinking feature allows AI models to take more time for complex reasoning:

### Timeout Settings
- **connect**: Time to establish HTTP connection
- **read**: Time to wait for API response
- **write**: Time to send request data
- **long_thinking**: Enable/disable extended thinking mode

### Recommended Settings by Model Type

#### Fast Models (GPT-3.5, Basic APIs)
```yaml
timeout:
  connect: 30
  read: 60
  write: 30
  long_thinking: false
```

#### Balanced Models (GPT-4, Claude Haiku)
```yaml
timeout:
  connect: 30
  read: 120
  write: 30
  long_thinking: true
```

#### Thinking Models (Claude Opus, DeepSeek R1)
```yaml
timeout:
  connect: 30
  read: 300
  write: 30
  long_thinking: true
```

#### Research Models (o1-preview, o1-mini)
```yaml
timeout:
  connect: 30
  read: 600
  write: 30
  long_thinking: true
```

## Usage

### Configuration
1. **Configure API**: Add your API configuration in config.yml
2. **Set Current Model**: Modify `current_model` to your API name
3. **Reload Plugin**: Use `/gcc reload` to reload configuration
4. **Switch Models**: Use `/gcc model <api_name>` to switch APIs

### Commands
- `/gcc model api1` - Switch to api1 interface
- `/gcc model api2` - Switch to api2 interface
- `/gcc models` - View all available API interfaces
- `/gcc reload` - Reload configuration file
- `/gcc timeout list` - View timeout settings for all models
- `/gcc timeout info <model>` - View detailed timeout info
- `/gcc timeout toggle <model>` - Toggle long thinking mode

## Best Practices

### Security
1. **API Key Safety**: Keep your API keys secure
2. **Environment Variables**: Consider using environment variables for sensitive data
3. **Access Control**: Use proper permissions to restrict access

### Performance
1. **Timeout Tuning**: Adjust timeouts based on model performance
2. **Network Optimization**: Consider proxy settings for better connectivity
3. **Model Selection**: Choose appropriate models for different use cases

### Configuration
1. **Request Format**: Ensure body_template format is correct with valid JSON
2. **Response Path**: Ensure content_path points to the correct response field
3. **Error Handling**: Configure error_path for proper error display
4. **Parameter Types**: Values in parameters are automatically converted to corresponding JSON types

## Debugging

Enable debug mode to see detailed API requests and responses:

```yaml
debug:
  enabled: true
```

This will output in the console:
- Request URL
- Request body content
- Response status code
- Response content
- Timeout information

## Troubleshooting

### Common Issues

**Q: API not responding?**
A: Check:
1. API key is correct
2. Base URL is accessible
3. Request format matches API requirements
4. Network connectivity

**Q: Timeout errors?**
A: 
1. Increase read timeout
2. Enable long thinking mode
3. Check network stability
4. Verify API performance

**Q: Response parsing errors?**
A: 
1. Verify content_path syntax
2. Check API response format
3. Enable debug mode to see raw responses

**Q: Authentication errors?**
A: 
1. Verify API key format
2. Check header configuration
3. Ensure API key has proper permissions

## Supported Features

✅ **Fully Customizable API Interfaces**
✅ **Flexible Request Header Configuration**
✅ **Variable Substitution System**
✅ **JSON Path Response Parsing**
✅ **Additional Parameter Support**
✅ **Error Handling**
✅ **History Management**
✅ **Persona System Integration**
✅ **Long Thinking Mode**
✅ **Real-time Configuration**

---

**Happy configuring! 🚀**
