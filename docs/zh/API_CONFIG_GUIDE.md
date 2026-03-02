# GeminiCraftChat API配置指南

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
      
      # 超时配置（长思考功能）
      timeout:
        connect: 30        # 连接超时（秒）
        read: 60          # 读取超时（秒）
        write: 30         # 写入超时（秒）
        long_thinking: false  # 启用长思考模式
      
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

### 基础变量
- `{api_key}` - API密钥
- `{model}` - 模型名称
- `{temperature}` - 温度值
- `{max_tokens}` - 最大令牌数
- `{messages}` - 聊天消息（JSON数组）

### 消息变量
- `{user_message}` - 当前用户消息
- `{system_prompt}` - 系统提示（人设）
- `{history}` - 聊天历史

### 自定义变量
你可以在`parameters`部分添加自定义变量，并使用`{参数名}`引用。

## 响应路径语法

使用JSON路径语法从API响应中提取内容：

### 简单路径
```yaml
content_path: "content"  # response.content
```

### 嵌套路径
```yaml
content_path: "choices[0].message.content"  # response.choices[0].message.content
```

### 数组索引
```yaml
content_path: "data[0].text"  # response.data[0].text
```

## API示例

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
    long_thinking: true  # Claude受益于更长的思考时间
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
    read: 300  # R1需要更多推理时间
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

### 5. 自定义API
```yaml
api5:
  name: "自定义API"
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

## 长思考配置

长思考功能允许AI模型花费更多时间进行复杂推理：

### 超时设置
- **connect**: 建立HTTP连接的时间
- **read**: 等待API响应的时间
- **write**: 发送请求数据的时间
- **long_thinking**: 启用/禁用扩展思考模式

### 按模型类型推荐设置

#### 快速模型（GPT-3.5，基础API）
```yaml
timeout:
  connect: 30
  read: 60
  write: 30
  long_thinking: false
```

#### 平衡模型（GPT-4，Claude Haiku）
```yaml
timeout:
  connect: 30
  read: 120
  write: 30
  long_thinking: true
```

#### 思考模型（Claude Opus，DeepSeek R1）
```yaml
timeout:
  connect: 30
  read: 300
  write: 30
  long_thinking: true
```

#### 研究模型（o1-preview，o1-mini）
```yaml
timeout:
  connect: 30
  read: 600
  write: 30
  long_thinking: true
```

## 使用方法

### 配置
1. **配置API**: 在config.yml中添加你的API配置
2. **设置当前模型**: 修改`current_model`为你的API名称
3. **重载插件**: 使用`/gcc reload`重载配置
4. **切换模型**: 使用`/gcc model <api_name>`切换API

### 命令
- `/gcc model api1` - 切换到api1接口
- `/gcc model api2` - 切换到api2接口
- `/gcc models` - 查看所有可用的API接口
- `/gcc reload` - 重载配置文件
- `/gcc timeout list` - 查看所有模型的超时设置
- `/gcc timeout info <模型>` - 查看详细超时信息
- `/gcc timeout toggle <模型>` - 切换长思考模式

## 最佳实践

### 安全性
1. **API密钥安全**: 请妥善保管你的API密钥
2. **环境变量**: 考虑使用环境变量存储敏感数据
3. **访问控制**: 使用适当的权限限制访问

### 性能
1. **超时调优**: 根据模型性能调整超时时间
2. **网络优化**: 考虑代理设置以获得更好的连接性
3. **模型选择**: 为不同用例选择合适的模型

### 配置
1. **请求格式**: 确保body_template格式正确，使用有效的JSON
2. **响应路径**: 确保content_path指向正确的响应字段
3. **错误处理**: 配置error_path以便正确显示错误信息
4. **参数类型**: parameters中的值会自动转换为对应的JSON类型

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
- 超时信息

## 故障排除

### 常见问题

**Q: API没有响应？**
A: 检查：
1. API密钥是否正确
2. 基础URL是否可访问
3. 请求格式是否符合API要求
4. 网络连接性

**Q: 超时错误？**
A: 
1. 增加读取超时时间
2. 启用长思考模式
3. 检查网络稳定性
4. 验证API性能

**Q: 响应解析错误？**
A: 
1. 验证content_path语法
2. 检查API响应格式
3. 启用调试模式查看原始响应

**Q: 认证错误？**
A: 
1. 验证API密钥格式
2. 检查头部配置
3. 确保API密钥有适当权限

## 支持的功能

✅ **完全自定义API接口**
✅ **灵活的请求头配置**
✅ **变量替换系统**
✅ **JSON路径响应解析**
✅ **额外参数支持**
✅ **错误处理**
✅ **历史记录管理**
✅ **人设系统集成**
✅ **长思考模式**
✅ **实时配置**

---

**配置愉快！🚀**
