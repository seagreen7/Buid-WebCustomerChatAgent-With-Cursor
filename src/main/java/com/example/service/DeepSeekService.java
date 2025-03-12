package com.example.service;

import com.example.dto.MessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${deepseek.api.url}")
    private String apiUrl;
    
    @Value("${deepseek.api.key}")
    private String apiKey;
    
    private String faqContext = "";
    
    @PostConstruct
    public void init() {
        loadFaqContext();
        logger.info("DeepSeekService initialized with API URL: {}", apiUrl);
    }
    
    private void loadFaqContext() {
        try {
            Path faqPath = Paths.get("faq.txt");
            if (Files.exists(faqPath)) {
                faqContext = new String(Files.readAllBytes(faqPath));
                logger.info("FAQ context loaded successfully, length: {} characters", faqContext.length());
            } else {
                logger.warn("faq.txt not found in root directory");
            }
        } catch (IOException e) {
            logger.error("Error loading FAQ context", e);
        }
    }
    
    public String generateResponse(List<MessageDto> messages) {
        try {
            logger.debug("Generating response for {} messages", messages.size());
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost request = new HttpPost(apiUrl);
            
            // Set headers
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + apiKey);
            
            // Prepare the system message with the FAQ context
            MessageDto systemMessage = new MessageDto();
            systemMessage.setRole("system");
            systemMessage.setContent("You are a helpful customer service assistant. Use the following FAQ information to help answer questions: " + faqContext);
            
            // Create the complete message list with system message first
            List<MessageDto> completeMessages = new ArrayList<>();
            completeMessages.add(systemMessage);
            completeMessages.addAll(messages);
            
            // Prepare request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("stream", false);
            
            ArrayNode messagesArray = requestBody.putArray("messages");
            for (MessageDto message : completeMessages) {
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", message.getRole());
                messageNode.put("content", message.getContent());
            }
            
            String requestJson = requestBody.toString();
            logger.debug("Sending request to DeepSeek API: {}", requestJson);
            
            // Execute request
            request.setEntity(new StringEntity(requestJson, "UTF-8"));
            HttpResponse response = httpClient.execute(request);
            
            // Process response
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            logger.debug("Received response from DeepSeek API. Status: {}, Body: {}", statusCode, responseBody);
            
            if (statusCode != 200) {
                logger.error("DeepSeek API error. Status: {}, Response: {}", statusCode, responseBody);
                return "抱歉，我遇到了一些技术问题，请稍后再试。";
            }
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            String content = jsonResponse.path("choices").path(0).path("message").path("content").asText();
            logger.debug("Extracted response content: {}", content);
            
            return content;
            
        } catch (Exception e) {
            logger.error("Error generating response", e);
            return "抱歉，我遇到了一个错误: " + e.getMessage();
        }
    }
} 