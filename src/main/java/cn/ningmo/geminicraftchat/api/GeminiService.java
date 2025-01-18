package cn.ningmo.geminicraftchat.api;

import cn.ningmo.geminicraftchat.GeminiCraftChat;
import cn.ningmo.geminicraftchat.config.ConfigManager;
import cn.ningmo.geminicraftchat.persona.Persona;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

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
    private final String apiEndpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent";

    public GeminiService(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatHistories = new ConcurrentHashMap<>();
        this.gson = new Gson();
    }

    public CompletableFuture<String> sendMessage(String playerId, String message, Optional<Persona> persona) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, String>> history = chatHistories.computeIfAbsent(playerId, k -> new ArrayList<>());
                
                // 构建请求体
                JsonObject requestBody = new JsonObject();
                JsonArray contents = new JsonArray();
                
                // 如果有人设且历史记录为空，添加人设上下文
                if (persona.isPresent() && history.isEmpty()) {
                    JsonObject personaContent = new JsonObject();
                    JsonArray parts = new JsonArray();
                    JsonObject part = new JsonObject();
                    part.addProperty("text", persona.get().getContext());
                    parts.add(part);
                    personaContent.add("parts", parts);
                    personaContent.addProperty("role", "user");
                    contents.add(personaContent);
                }

                // 添加用户消息
                JsonObject userContent = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", message);
                parts.add(part);
                userContent.add("parts", parts);
                userContent.addProperty("role", "user");
                contents.add(userContent);

                requestBody.add("contents", contents);
                
                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("temperature", 0.7);
                generationConfig.addProperty("topP", 0.95);
                generationConfig.addProperty("topK", 40);
                generationConfig.addProperty("maxOutputTokens", 1024);
                requestBody.add("generationConfig", generationConfig);

                // 发送请求
                String response = sendRequest(requestBody.toString());
                
                // 解析响应
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                String responseText = jsonResponse
                    .getAsJsonArray("candidates")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0)
                    .getAsJsonObject()
                    .get("text")
                    .getAsString();

                // 添加AI响应到历史记录
                Map<String, String> aiMessage = new HashMap<>();
                aiMessage.put("role", "assistant");
                aiMessage.put("content", responseText);
                history.add(aiMessage);
                
                // 限制历史记录长度
                if (history.size() > configManager.getMaxHistory()) {
                    history.subList(0, history.size() - configManager.getMaxHistory()).clear();
                }
                
                return responseText;
            } catch (Exception e) {
                plugin.getLogger().warning("发送消息到 Gemini API 失败: " + e.getMessage());
                throw new RuntimeException("AI 响应失败", e);
            }
        });
    }

    private String sendRequest(String requestBody) throws IOException {
        String apiKey = configManager.getApiKey();
        URL url = new URL(apiEndpoint + "?key=" + apiKey);
        
        if (plugin.isDebugEnabled()) {
            plugin.debug("发送请求到: " + url);
            plugin.debug("请求体: " + requestBody);
        }
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBody);
            writer.flush();
        }

        if (conn.getResponseCode() != 200) {
            try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
                StringBuilder error = new StringBuilder();
                while (scanner.hasNextLine()) {
                    error.append(scanner.nextLine());
                }
                plugin.getLogger().warning("API 错误响应: " + error.toString());
            }
            throw new IOException("Server returned HTTP response code: " + conn.getResponseCode());
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.debug("API 响应: " + response.toString());
        }

        return response.toString();
    }

    public void clearHistory(String playerId) {
        chatHistories.remove(playerId);
    }

    public void clearAllHistory() {
        chatHistories.clear();
    }
} 