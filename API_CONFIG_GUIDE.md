# GeminiCraftChat API配置指南

> **注意**: 这是中文版文档。[English version](docs/en/API_CONFIG_GUIDE.md) | [返回文档首页](docs/README.md)

## 概述

GeminiCraftChat现在支持完全可配置的API接口，你可以在配置文件中自定义任何AI服务商的API调用方式，无需修改代码。

## 配置结构

每个API接口的配置包含以下部分：

```yaml
api:
  models:
    your_api_name:  # 自定义API名称
      name: "显示名称"
      model: "模型名称"
      base_url: "API地址"
      api_key: "API密钥"
      max_tokens: 4096
      temperature: 0.7
      
      # 请求配置
      request:
        method: "POST"  # 请求方法
        headers:        # 请求头
          "Content-Type": "application/json"
          "Authorization": "Bearer {api_key}"
        body_template: |  # 请求体模板
          {
            "model": "{model}",
            "messages": {messages},
            "temperature": {temperature},
            "max_tokens": {max_tokens}
          }
      
      # 响应解析配置
      response:
        content_path: "choices[0].message.content"  # 响应内容路径
        error_path: "error.message"                 # 错误信息路径
      
      # 额外参数
      parameters:
        frequency_penalty: 0.0
        presence_penalty: 0.0
```

## 变量替换

在配置中可以使用以下变量：

- `{api_key}` - API密钥
- `{model}` - 模型名称
- `{messages}` - 消息数组（自动生成）
- `{temperature}` - 温度参数
- `{max_tokens}` - 最大令牌数

## 响应路径语法

使用点号分隔的JSON路径来指定响应内容位置：

- `choices[0].message.content` - 访问choices数组第0个元素的message.content
- `content[0].text` - 访问content数组第0个元素的text
- `output.choices[0].message.content` - 嵌套路径

## 常见API配置示例

### OpenAI格式
```yaml
openai_api:
  name: "OpenAI GPT"
  model: "gpt-3.5-turbo"
  base_url: "https://api.openai.com/v1/chat/completions"
  api_key: "sk-your-key-here"
  request:
    headers:
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
```

### Claude格式
```yaml
claude_api:
  name: "Claude AI"
  model: "claude-3-sonnet-20240229"
  base_url: "https://api.anthropic.com/v1/messages"
  api_key: "sk-ant-your-key-here"
  request:
    headers:
      "x-api-key": "{api_key}"
      "anthropic-version": "2023-06-01"
    body_template: |
      {
        "model": "{model}",
        "messages": {messages},
        "max_tokens": {max_tokens}
      }
  response:
    content_path: "content[0].text"
```

### 通义千问格式
```yaml
qwen_api:
  name: "通义千问"
  model: "qwen-turbo"
  base_url: "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
  api_key: "sk-your-key-here"
  request:
    headers:
      "Authorization": "Bearer {api_key}"
    body_template: |
      {
        "model": "{model}",
        "input": {
          "messages": {messages}
        },
        "parameters": {
          "temperature": {temperature},
          "max_tokens": {max_tokens}
        }
      }
  response:
    content_path: "output.choices[0].message.content"
```

## 使用方法

1. **配置API**: 在config.yml中添加你的API配置
2. **设置当前模型**: 修改`current_model`为你的API名称
3. **重载插件**: 使用`/gcc reload`重载配置
4. **切换模型**: 使用`/gcc model <api_name>`切换API

## 命令

- `/gcc model api1` - 切换到api1接口
- `/gcc model api2` - 切换到api2接口
- `/gcc models` - 查看所有可用的API接口
- `/gcc reload` - 重载配置文件

## 注意事项

1. **API密钥安全**: 请妥善保管你的API密钥
2. **请求格式**: 确保body_template格式正确，使用有效的JSON
3. **响应路径**: 确保content_path指向正确的响应字段
4. **错误处理**: 配置error_path以便正确显示错误信息
5. **参数类型**: parameters中的值会自动转换为对应的JSON类型

## 调试

启用调试模式可以查看详细的API请求和响应：

```yaml
debug:
  enabled: true
```

这将在控制台输出：
- 请求URL
- 请求体内容
- 响应状态码
- 响应内容

## 支持的功能

✅ **完全自定义API接口**
✅ **灵活的请求头配置**
✅ **变量替换系统**
✅ **JSON路径响应解析**
✅ **额外参数支持**
✅ **错误处理**
✅ **历史记录管理**
✅ **人设系统集成**
