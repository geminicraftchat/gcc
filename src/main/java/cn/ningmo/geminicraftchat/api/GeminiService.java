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
    private final Map<String, List<Map<String, String>>> chatHistories;
    private final Gson gson;
    private final String apiEndpoint = "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent";

    public GeminiService(GeminiCraftChat plugin) {
        this.plugin = plugin;
        this.chatHistories = new ConcurrentHashMap<>();
        this.gson = new Gson();
    }

    public CompletableFuture<String> sendMessage(String playerId, String message, Optional<Persona> persona) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, String>> history = chatHistories.computeIfAbsent(playerId, k -> new ArrayList<>());
                
                // 如果有人设且历史记录为空，添加人设上下文
                if (persona.isPresent() && history.isEmpty()) {
                    Map<String, String> personaMessage = new HashMap<>();
                    personaMessage.put("role", "user");
                    personaMessage.put("content", persona.get().getContext());
                    history.add(personaMessage);
                }

                // 添加用户消息
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", message);
                history.add(userMessage);

                // 构建请求体
                JsonObject requestBody = new JsonObject();
                JsonArray contents = new JsonArray();
                
                for (Map<String, String> msg : history) {
                    JsonObject content = new JsonObject();
                    content.addProperty("role", msg.get("role"));
                    content.addProperty("parts", msg.get("content"));
                    contents.add(content);
                }
                
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
                if (history.size() > 10) {
                    history.subList(0, history.size() - 10).clear();
                }
                
                return responseText;
            } catch (Exception e) {
                plugin.getLogger().warning("发送消息到 Gemini API 失败: " + e.getMessage());
                throw new RuntimeException("AI 响应失败", e);
            }
        });
    }

    private String sendRequest(String requestBody) throws IOException {
        ConfigManager config = plugin.getConfigManager();
        String apiKey = config.getApiKey();
        URL url = new URL(apiEndpoint + "?key=" + apiKey);
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBody);
            writer.flush();
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
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