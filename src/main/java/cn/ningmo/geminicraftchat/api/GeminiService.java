package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.persona.Persona;
import cn.ningmo.geminicraftchat.constants.ProxyType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GeminiService {
    private final GeminiCraftChat plugin;
    private final ConfigManager configManager;
    private final Map<String, List<Map<String, String>>> chatHistories;
    private final Gson gson;

    public GeminiService(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatHistories = new ConcurrentHashMap<>();
        this.gson = new Gson();
    }

    public CompletableFuture<String> sendMessage(String playerId, String message, Optional<Persona> persona) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String currentModel = configManager.getCurrentModel();
                if ("deepseek".equals(currentModel)) {
                    return sendDeepSeekRequest(playerId, message, persona);
                } else if ("gemini".equals(currentModel)) {
                    return sendGeminiRequest(playerId, message, persona);
                } else {
                    throw new IllegalStateException("未知的模型类型: " + currentModel);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("发送消息失败: " + e.getMessage());
                throw new RuntimeException("AI 响应失败", e);
            }
        });
    }

    private String sendDeepSeekRequest(String playerId, String message, Optional<Persona> persona) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", configManager.getModel());
        
        // 构建消息数组
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
        
        requestBody.add("messages", messages);
        
        // 添加DeepSeek特定参数
        requestBody.addProperty("temperature", configManager.getTemperature());
        requestBody.addProperty("max_tokens", configManager.getMaxTokens());
        
        // 添加额外参数
        ConfigurationSection parameters = configManager.getConfig().getConfigurationSection("api.models.deepseek.parameters");
        if (parameters != null) {
            for (String key : parameters.getKeys(false)) {
                Object value = parameters.get(key);
                if (value != null) {
                    if (value instanceof Boolean) {
                        requestBody.addProperty(key, (Boolean) value);
                    } else if (value instanceof Number) {
                        requestBody.addProperty(key, (Number) value);
                    } else if (value instanceof String) {
                        requestBody.addProperty(key, (String) value);
                    }
                }
            }
        }
        
        // 发送请求
        String url = configManager.getBaseUrl();
        plugin.debug("发送DeepSeek请求到: " + url);
        plugin.debug("请求体: " + requestBody.toString());
        
        HttpURLConnection conn = createConnection(new URL(url));
        conn.setRequestProperty("Authorization", "Bearer " + configManager.getApiKey());
        
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody.toString());
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            plugin.debug("API响应代码: " + responseCode);

            if (responseCode != 200) {
                handleErrorResponse(conn);
            }

            String response = readResponse(conn);
            plugin.debug("API原始响应: " + response);

            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String responseText = jsonResponse
                .getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("message")
                .get("content")
                .getAsString();

            // 更新历史记录
            updateChatHistory(playerId, message, responseText);
            plugin.debug("更新历史记录成功");

            return responseText;
        } finally {
            conn.disconnect();
        }
    }

    private String sendGeminiRequest(String playerId, String message, Optional<Persona> persona) throws IOException {
        String requestFormat = configManager.getConfig().getString("api.models.gemini.request_format", "openai");
        
        JsonObject requestBody = new JsonObject();
        
        if ("openai".equalsIgnoreCase(requestFormat)) {
            // 使用 OpenAI 格式
            requestBody.addProperty("model", configManager.getModel());
            
            // 构建消息数组
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
            
            requestBody.add("messages", messages);
            
            // 添加参数
            requestBody.addProperty("temperature", configManager.getTemperature());
            requestBody.addProperty("max_tokens", configManager.getMaxTokens());
            
            // 添加额外参数
            ConfigurationSection parameters = configManager.getConfig().getConfigurationSection("api.models.gemini.parameters");
            if (parameters != null) {
                for (String key : parameters.getKeys(false)) {
                    Object value = parameters.get(key);
                    if (value != null) {
                        if (value instanceof Boolean) {
                            requestBody.addProperty(key, (Boolean) value);
                        } else if (value instanceof Number) {
                            requestBody.addProperty(key, (Number) value);
                        } else if (value instanceof String) {
                            requestBody.addProperty(key, (String) value);
                        }
                    }
                }
            }
        } else {
            // 使用原生 Gemini 格式
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            
            // 添加人设上下文
            if (persona.isPresent()) {
                JsonObject contextPart = new JsonObject();
                contextPart.addProperty("text", persona.get().getContext());
                parts.add(contextPart);
            }
            
            // 添加历史记录
            List<Map<String, String>> history = chatHistories.get(playerId);
            if (history != null) {
                for (Map<String, String> msg : history) {
                    JsonObject historyPart = new JsonObject();
                    historyPart.addProperty("text", msg.get("content"));
                    parts.add(historyPart);
                }
            }
            
            // 添加当前消息
            JsonObject messagePart = new JsonObject();
            messagePart.addProperty("text", message);
            parts.add(messagePart);
            
            content.add("parts", parts);
            
            JsonArray contents = new JsonArray();
            contents.add(content);
            requestBody.add("contents", contents);
            
            // 添加生成配置
            JsonObject generationConfig = new JsonObject();
            ConfigurationSection parameters = configManager.getConfig().getConfigurationSection("api.models.gemini.parameters");
            if (parameters != null) {
                for (String key : parameters.getKeys(false)) {
                    Object value = parameters.get(key);
                    if (value != null) {
                        if (value instanceof Boolean) {
                            generationConfig.addProperty(key, (Boolean) value);
                        } else if (value instanceof Number) {
                            generationConfig.addProperty(key, (Number) value);
                        } else if (value instanceof String) {
                            generationConfig.addProperty(key, (String) value);
                        }
                    }
                }
            }
            requestBody.add("generationConfig", generationConfig);
        }

        // 发送请求
        String url = configManager.getBaseUrl();
        plugin.debug("发送Gemini请求到: " + url);
        plugin.debug("请求体: " + requestBody.toString());

        HttpURLConnection conn = createConnection(new URL(url));
        conn.setRequestProperty("Authorization", "Bearer " + configManager.getApiKey());

        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(requestBody.toString());
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            plugin.debug("API响应代码: " + responseCode);

            if (responseCode != 200) {
                handleErrorResponse(conn);
            }

            String response = readResponse(conn);
            plugin.debug("API原始响应: " + response);

            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String responseText;
            
            if ("openai".equalsIgnoreCase(requestFormat)) {
                responseText = jsonResponse
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
            } else {
                responseText = jsonResponse
                    .getAsJsonArray("candidates")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0)
                    .getAsJsonObject()
                    .get("text")
                    .getAsString();
            }

            // 更新历史记录
            updateChatHistory(playerId, message, responseText);
            plugin.debug("更新历史记录成功");

            return responseText;
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection conn;
        
        if (configManager.isHttpProxyEnabled()) {
            String proxyHost = configManager.getHttpProxyHost();
            int proxyPort = configManager.getHttpProxyPort();
            ProxyType proxyType = ProxyType.fromString(configManager.getHttpProxyType());
            
            plugin.debug("使用" + proxyType.getValue() + "代理: " + proxyHost + ":" + proxyPort);
            
            switch (proxyType) {
                case SOCKS:
                    System.setProperty("socksProxyHost", proxyHost);
                    System.setProperty("socksProxyPort", String.valueOf(proxyPort));
                    break;
                case HTTP:
                default:
                    System.setProperty("http.proxyHost", proxyHost);
                    System.setProperty("http.proxyPort", String.valueOf(proxyPort));
                    System.setProperty("https.proxyHost", proxyHost);
                    System.setProperty("https.proxyPort", String.valueOf(proxyPort));
                    break;
            }
            
            conn = (HttpURLConnection) url.openConnection();
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        
        return conn;
    }

    private void handleErrorResponse(HttpURLConnection conn) throws IOException {
        StringBuilder error = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                error.append(scanner.nextLine());
            }
        }
        String errorMessage = error.toString();
        plugin.debug("API错误响应: " + errorMessage);
        plugin.getLogger().warning("API 错误响应: " + errorMessage);
        throw new IOException("Server returned HTTP response code: " + conn.getResponseCode() + "\n" + errorMessage);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }
        return response.toString();
    }

    private void updateChatHistory(String playerId, String message, String response) {
        List<Map<String, String>> history = chatHistories.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        history.add(userMessage);

        Map<String, String> aiMessage = new HashMap<>();
        aiMessage.put("role", "assistant");
        aiMessage.put("content", response);
        history.add(aiMessage);

        if (history.size() > configManager.getMaxHistory() * 2) {
            history.subList(0, 2).clear();
        }
    }

    public void clearHistory(String playerId) {
        chatHistories.remove(playerId);
    }

    public void clearAllHistory() {
        chatHistories.clear();
    }
} 