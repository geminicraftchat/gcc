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
    private final String directApiEndpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent";

    public GeminiService(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatHistories = new ConcurrentHashMap<>();
        this.gson = new Gson();
    }

    public CompletableFuture<String> sendMessage(String playerId, String message, Optional<Persona> persona) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (configManager.isProxyApi()) {
                    return sendProxyRequest(playerId, message, persona);
                } else {
                    return sendDirectRequest(playerId, message, persona);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("发送消息失败: " + e.getMessage());
                throw new RuntimeException("AI 响应失败", e);
            }
        });
    }

    private String sendProxyRequest(String playerId, String message, Optional<Persona> persona) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("message", message);
        
        // 添加人设信息
        if (persona.isPresent()) {
            requestBody.addProperty("persona", persona.get().getContext());
        }
        
        // 添加历史记录
        List<Map<String, String>> history = chatHistories.get(playerId);
        if (history != null && !history.isEmpty()) {
            JsonArray historyArray = new JsonArray();
            for (Map<String, String> msg : history) {
                JsonObject historyMsg = new JsonObject();
                historyMsg.addProperty("role", msg.get("role"));
                historyMsg.addProperty("content", msg.get("content"));
                historyArray.add(historyMsg);
            }
            requestBody.add("history", historyArray);
        }

        // 发送请求
        URL url = new URL(configManager.getProxyApiUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + configManager.getProxyApiKey());
        
        // 设置代理（如果启用）
        if (configManager.isHttpProxyEnabled()) {
            System.setProperty("http.proxyHost", configManager.getHttpProxyHost());
            System.setProperty("http.proxyPort", String.valueOf(configManager.getHttpProxyPort()));
            System.setProperty("https.proxyHost", configManager.getHttpProxyHost());
            System.setProperty("https.proxyPort", String.valueOf(configManager.getHttpProxyPort()));
        }

        conn.setDoOutput(true);
        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBody.toString());
            writer.flush();
        }

        // 处理响应
        if (conn.getResponseCode() != 200) {
            handleErrorResponse(conn);
        }

        String response = readResponse(conn);
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        
        // 更新历史记录
        updateChatHistory(playerId, message, jsonResponse.get("response").getAsString());

        return jsonResponse.get("response").getAsString();
    }

    private String sendDirectRequest(String playerId, String message, Optional<Persona> persona) throws IOException {
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
        URL url = new URL(directApiEndpoint + "?key=" + configManager.getApiKey());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        
        // 设置代理（如果启用）
        if (configManager.isHttpProxyEnabled()) {
            System.setProperty("http.proxyHost", configManager.getHttpProxyHost());
            System.setProperty("http.proxyPort", String.valueOf(configManager.getHttpProxyPort()));
            System.setProperty("https.proxyHost", configManager.getHttpProxyHost());
            System.setProperty("https.proxyPort", String.valueOf(configManager.getHttpProxyPort()));
        }

        conn.setDoOutput(true);
        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBody.toString());
            writer.flush();
        }

        // 处理响应
        if (conn.getResponseCode() != 200) {
            handleErrorResponse(conn);
        }

        String response = readResponse(conn);
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

        // 更新历史记录
        updateChatHistory(playerId, message, responseText);

        return responseText;
    }

    private void handleErrorResponse(HttpURLConnection conn) throws IOException {
        try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
            StringBuilder error = new StringBuilder();
            while (scanner.hasNextLine()) {
                error.append(scanner.nextLine());
            }
            plugin.getLogger().warning("API 错误响应: " + error.toString());
            throw new IOException("Server returned HTTP response code: " + conn.getResponseCode());
        }
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