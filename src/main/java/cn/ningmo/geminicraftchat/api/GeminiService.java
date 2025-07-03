package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.persona.Persona;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GeminiService {
    private final GeminiCraftChat plugin;
    private final ConfigManager configManager;
    private final Gson gson;
    private final OkHttpClient baseClient;
    private final Map<String, OkHttpClient> modelClients;
    private final Map<String, List<Map<String, String>>> chatHistories;

    public GeminiService(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.gson = new Gson();
        this.baseClient = createBaseHttpClient();
        this.modelClients = new ConcurrentHashMap<>();
        this.chatHistories = new ConcurrentHashMap<>();
    }

    private OkHttpClient createBaseHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS);

        // 配置代理
        if (configManager.isHttpProxyEnabled()) {
            String proxyHost = configManager.getHttpProxyHost();
            int proxyPort = configManager.getHttpProxyPort();
            String proxyType = configManager.getHttpProxyType();

            plugin.debug("使用" + proxyType + "代理: " + proxyHost + ":" + proxyPort);

            Proxy.Type type = "SOCKS".equalsIgnoreCase(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
            builder.proxy(proxy);
        }

        return builder.build();
    }

    /**
     * 为特定模型创建HTTP客户端，支持自定义超时设置
     */
    private OkHttpClient createModelHttpClient(String modelKey) {
        ConfigurationSection modelConfig = configManager.getConfig().getConfigurationSection("api.models." + modelKey);
        if (modelConfig == null) {
            plugin.debug("模型 " + modelKey + " 配置不存在，使用基础客户端");
            return baseClient;
        }

        // 获取超时配置
        ConfigurationSection timeoutConfig = modelConfig.getConfigurationSection("timeout");
        if (timeoutConfig == null) {
            plugin.debug("模型 " + modelKey + " 未配置超时设置，使用基础客户端");
            return baseClient;
        }

        int connectTimeout = timeoutConfig.getInt("connect", 30);
        int readTimeout = timeoutConfig.getInt("read", 30);
        int writeTimeout = timeoutConfig.getInt("write", 30);
        boolean longThinking = timeoutConfig.getBoolean("long_thinking", false);

        // 如果启用长思考模式，增加读取超时时间
        if (longThinking) {
            plugin.debug("模型 " + modelKey + " 启用长思考模式，读取超时: " + readTimeout + "秒");
        }

        OkHttpClient.Builder builder = baseClient.newBuilder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS);

        plugin.debug("为模型 " + modelKey + " 创建专用客户端 - 连接:" + connectTimeout + "s, 读取:" + readTimeout + "s, 写入:" + writeTimeout + "s");

        return builder.build();
    }

    /**
     * 获取或创建模型专用的HTTP客户端
     */
    private OkHttpClient getModelClient(String modelKey) {
        return modelClients.computeIfAbsent(modelKey, this::createModelHttpClient);
    }

    public CompletableFuture<String> sendMessage(String playerId, String message, Optional<Persona> persona) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String currentModel = configManager.getCurrentModel();
                return sendGenericRequest(playerId, message, persona, currentModel);
            } catch (Exception e) {
                plugin.getLogger().warning("发送消息失败: " + e.getMessage());
                throw new RuntimeException("AI 响应失败", e);
            }
        });
    }

    /**
     * 通用API请求方法 - 完全基于配置文件
     */
    private String sendGenericRequest(String playerId, String message, Optional<Persona> persona, String modelKey) throws IOException {
        long startTime = System.currentTimeMillis();

        ConfigurationSection modelConfig = configManager.getConfig().getConfigurationSection("api.models." + modelKey);
        if (modelConfig == null) {
            throw new IllegalStateException("模型配置不存在: " + modelKey);
        }

        // 获取基本配置
        String baseUrl = modelConfig.getString("base_url");
        String apiKey = modelConfig.getString("api_key");
        String model = modelConfig.getString("model");
        String modelName = modelConfig.getString("name", modelKey);
        double temperature = modelConfig.getDouble("temperature", 0.7);
        int maxTokens = modelConfig.getInt("max_tokens", 4096);

        // 获取请求配置
        ConfigurationSection requestConfig = modelConfig.getConfigurationSection("request");
        if (requestConfig == null) {
            throw new IllegalStateException("请求配置不存在: " + modelKey);
        }

        String method = requestConfig.getString("method", "POST");
        String bodyTemplate = requestConfig.getString("body_template", "{}");

        // 构建消息数组
        JsonArray messages = buildMessagesArray(playerId, message, persona);

        // 替换模板变量
        String requestBody = bodyTemplate
            .replace("{model}", model)
            .replace("{messages}", gson.toJson(messages))
            .replace("{temperature}", String.valueOf(temperature))
            .replace("{max_tokens}", String.valueOf(maxTokens))
            .replace("{api_key}", apiKey);

        // 添加额外参数
        JsonObject bodyJson = gson.fromJson(requestBody, JsonObject.class);
        ConfigurationSection parameters = modelConfig.getConfigurationSection("parameters");
        if (parameters != null) {
            addParametersToRequestBody(bodyJson, parameters);
        }

        plugin.debug("发送" + modelName + "请求到: " + baseUrl);
        plugin.debug("请求体: " + gson.toJson(bodyJson));

        // 构建请求
        Request.Builder requestBuilder = new Request.Builder().url(baseUrl);
        Map<String, String> requestHeaders = new HashMap<>();

        // 添加请求头
        ConfigurationSection headers = requestConfig.getConfigurationSection("headers");
        if (headers != null) {
            for (String headerName : headers.getKeys(false)) {
                String headerValue = headers.getString(headerName);
                if (headerValue != null) {
                    // 替换头部变量
                    headerValue = headerValue.replace("{api_key}", apiKey);
                    requestBuilder.addHeader(headerName, headerValue);
                    requestHeaders.put(headerName, headerValue);
                }
            }
        }

        // 记录API请求
        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logApiRequest(playerId, modelName, baseUrl, requestHeaders, gson.toJson(bodyJson));
        }

        // 设置请求体
        if ("POST".equalsIgnoreCase(method)) {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            requestBuilder.post(RequestBody.create(gson.toJson(bodyJson), JSON));
        }

        Request request = requestBuilder.build();

        // 获取模型专用的HTTP客户端
        OkHttpClient modelClient = getModelClient(modelKey);

        // 记录超时设置信息
        ConfigurationSection timeoutConfig = modelConfig.getConfigurationSection("timeout");
        if (timeoutConfig != null && timeoutConfig.getBoolean("long_thinking", false)) {
            plugin.debug("使用长思考模式发送请求到 " + modelName + "，读取超时: " + timeoutConfig.getInt("read", 30) + "秒");
        }

        // 发送请求并处理响应
        try (Response response = modelClient.newCall(request).execute()) {
            long responseTime = System.currentTimeMillis() - startTime;
            int responseCode = response.code();
            plugin.debug("API响应代码: " + responseCode);

            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // 记录失败的API调用
                if (plugin.getLogManager() != null) {
                    plugin.getLogManager().logApiCall(playerId, modelName, baseUrl, responseTime, false);
                    plugin.getLogManager().logApiResponse(playerId, modelName, responseCode, responseBody, "");
                    plugin.getLogManager().logError(playerId, "API请求失败: " + responseCode + " - " + responseBody);
                }
                throw new IOException("API请求失败: " + responseCode + " - " + responseBody);
            }

            plugin.debug("API原始响应: " + responseBody);

            // 解析响应
            String responseText = parseResponse(responseBody, modelConfig);

            // 记录成功的API调用
            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logApiCall(playerId, modelName, baseUrl, responseTime, true);
                plugin.getLogManager().logApiResponse(playerId, modelName, responseCode, responseBody, responseText);
                plugin.getLogManager().logChat(playerId, message, responseText);
            }

            // 更新历史记录
            updateChatHistory(playerId, message, responseText);
            plugin.debug("更新历史记录成功");

            return responseText;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;

            // 记录异常
            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logApiCall(playerId, modelName, baseUrl, responseTime, false);
                plugin.getLogManager().logError(playerId, "API调用异常: " + e.getMessage());
            }

            throw e;
        }
    }

    // 辅助方法：构建消息数组
    private JsonArray buildMessagesArray(String playerId, String message, Optional<Persona> persona) {
        JsonArray messages = new JsonArray();
        
        // 添加人设系统消息
        if (persona.isPresent()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", persona.get().getContext());
            messages.add(systemMessage);
        }
        
        // 添加历史记录
        List<Map<String, String>> history = chatHistories.get(playerId);
        if (history != null) {
            for (Map<String, String> msg : history) {
                JsonObject historyMessage = new JsonObject();
                historyMessage.addProperty("role", msg.get("role"));
                historyMessage.addProperty("content", msg.get("content"));
                messages.add(historyMessage);
            }
        }
        
        // 添加当前消息
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messages.add(userMessage);
        
        return messages;
    }

    // 辅助方法：添加参数到请求体
    private void addParametersToRequestBody(JsonObject requestBody, ConfigurationSection parameters) {
        for (String key : parameters.getKeys(false)) {
            Object value = parameters.get(key);
            addParameterToJson(requestBody, key, value);
        }
    }

    // 辅助方法：添加参数到JSON
    private void addParameterToJson(JsonObject json, String key, Object value) {
        if (value != null) {
            if (value instanceof Boolean) {
                json.addProperty(key, (Boolean) value);
            } else if (value instanceof Number) {
                json.addProperty(key, (Number) value);
            } else if (value instanceof String) {
                json.addProperty(key, (String) value);
            }
        }
    }

    // 解析响应内容
    private String parseResponse(String responseBody, ConfigurationSection modelConfig) throws IOException {
        ConfigurationSection responseConfig = modelConfig.getConfigurationSection("response");
        if (responseConfig == null) {
            throw new IllegalStateException("响应配置不存在");
        }
        
        String contentPath = responseConfig.getString("content_path", "choices[0].message.content");
        
        try {
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            return extractValueFromPath(jsonResponse, contentPath);
        } catch (Exception e) {
            plugin.getLogger().warning("解析响应失败: " + e.getMessage());
            throw new IOException("响应解析失败: " + e.getMessage());
        }
    }

    // 从JSON路径提取值
    private String extractValueFromPath(JsonObject json, String path) {
        String[] parts = path.split("\\.");
        JsonElement current = json;
        
        for (String part : parts) {
            if (part.contains("[") && part.contains("]")) {
                // 处理数组索引
                String arrayName = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                
                if (current.isJsonObject() && current.getAsJsonObject().has(arrayName)) {
                    current = current.getAsJsonObject().getAsJsonArray(arrayName).get(index);
                } else {
                    throw new RuntimeException("路径不存在: " + arrayName);
                }
            } else {
                // 处理普通属性
                if (current.isJsonObject() && current.getAsJsonObject().has(part)) {
                    current = current.getAsJsonObject().get(part);
                } else {
                    throw new RuntimeException("路径不存在: " + part);
                }
            }
        }
        
        return current.getAsString();
    }

    private void updateChatHistory(String playerId, String userMessage, String aiResponse) {
        List<Map<String, String>> history = chatHistories.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        // 添加用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);
        
        // 添加AI回复
        Map<String, String> aiMsg = new HashMap<>();
        aiMsg.put("role", "assistant");
        aiMsg.put("content", aiResponse);
        history.add(aiMsg);
        
        // 限制历史记录长度
        int maxHistory = configManager.getMaxHistory();
        while (history.size() > maxHistory * 2) {
            history.remove(0);
        }
    }

    public void clearHistory(String playerId) {
        chatHistories.remove(playerId);
    }

    public void clearAllHistory() {
        chatHistories.clear();
    }

    /**
     * 关闭HTTP客户端
     */
    public void shutdown() {
        try {
            // 关闭基础客户端
            shutdownClient(baseClient, "基础客户端");

            // 关闭所有模型专用客户端
            for (Map.Entry<String, OkHttpClient> entry : modelClients.entrySet()) {
                shutdownClient(entry.getValue(), "模型客户端 " + entry.getKey());
            }

            modelClients.clear();
            plugin.debug("GeminiService已关闭");
        } catch (Exception e) {
            plugin.getLogger().warning("关闭GeminiService时发生错误: " + e.getMessage());
        }
    }

    /**
     * 关闭单个HTTP客户端
     */
    private void shutdownClient(OkHttpClient client, String clientName) {
        try {
            // 关闭所有活动的连接
            client.dispatcher().cancelAll();

            // 关闭连接池
            client.connectionPool().evictAll();

            // 关闭线程池
            if (!client.dispatcher().executorService().isShutdown()) {
                client.dispatcher().executorService().shutdown();
                try {
                    if (!client.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)) {
                        client.dispatcher().executorService().shutdownNow();
                    }
                } catch (InterruptedException e) {
                    client.dispatcher().executorService().shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            plugin.debug(clientName + "已关闭");
        } catch (Exception e) {
            plugin.getLogger().warning("关闭" + clientName + "时发生错误: " + e.getMessage());
        }
    }
}
