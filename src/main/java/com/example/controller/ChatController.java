package com.example.controller;

import com.example.dto.MessageDto;
import com.example.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final DeepSeekService deepSeekService;
    private final Map<String, List<MessageDto>> sessionMessages = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ChatController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @CrossOrigin
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResponse(@RequestParam String sessionId, @RequestParam String message) {
        logger.debug("Received chat request - sessionId: {}, message: {}", sessionId, message);
        
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout
        
        // Add completion callback
        emitter.onCompletion(() -> {
            logger.debug("SSE connection completed for session: {}", sessionId);
        });
        
        // Add timeout callback
        emitter.onTimeout(() -> {
            logger.debug("SSE connection timed out for session: {}", sessionId);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("连接超时，请重试", MediaType.TEXT_PLAIN));
            } catch (Exception ex) {
                logger.error("Error sending timeout message", ex);
            }
        });
        
        // Add error callback
        emitter.onError(ex -> {
            logger.error("SSE connection error for session: {}", sessionId, ex);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("连接错误，请重试", MediaType.TEXT_PLAIN));
            } catch (Exception e) {
                logger.error("Error sending error message", e);
            }
        });
        
        // Add user message to session history
        MessageDto userMessage = new MessageDto("user", message);
        sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(userMessage);
        
        // Get current conversation history
        List<MessageDto> conversation = sessionMessages.get(sessionId);
        logger.debug("Current conversation size: {}", conversation.size());
        
        executorService.execute(() -> {
            try {
                logger.debug("Starting to process message in background thread for session: {}", sessionId);
                
                // Send "thinking" event
                emitter.send(SseEmitter.event()
                        .name("thinking")
                        .data("Thinking...", MediaType.TEXT_PLAIN));
                
                // Get response from DeepSeek
                logger.debug("Calling DeepSeek service for session: {}", sessionId);
                String response = deepSeekService.generateResponse(conversation);
                
                if (response == null || response.trim().isEmpty()) {
                    logger.error("Received empty response from DeepSeek for session: {}", sessionId);
                    handleError(emitter, new RuntimeException("收到空的响应"));
                    return;
                }
                
                logger.debug("Received response from DeepSeek for session: {}, length: {}", sessionId, response.length());
                
                // Add assistant's response to the conversation history
                MessageDto assistantMessage = new MessageDto("assistant", response);
                conversation.add(assistantMessage);
                
                // Send response as SSE event
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(response.replace("\n", "\\n"), MediaType.TEXT_PLAIN));
                
                // Add a small delay before completing the emitter
                Thread.sleep(500);
                
                emitter.complete();
                logger.debug("Successfully completed SSE response for session: {}", sessionId);
            } catch (IOException e) {
                logger.error("Error while processing chat message for session: {}", sessionId, e);
                handleError(emitter, e);
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while processing message for session: {}", sessionId, e);
                handleError(emitter, e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Unexpected error while processing chat message for session: {}", sessionId, e);
                handleError(emitter, e);
            }
        });
        
        return emitter;
    }
    
    private void handleError(SseEmitter emitter, Exception e) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("抱歉，发生了一个错误: " + e.getMessage(), MediaType.TEXT_PLAIN));
            Thread.sleep(100);
            emitter.completeWithError(e);
        } catch (Exception ex) {
            logger.error("Error while handling error", ex);
            emitter.completeWithError(ex);
        }
    }
    
    @CrossOrigin
    @PostMapping("/newSession")
    public String createNewSession() {
        String sessionId = "session_" + System.currentTimeMillis();
        sessionMessages.put(sessionId, new ArrayList<>());
        logger.debug("Created new session: {}", sessionId);
        return sessionId;
    }
    
    @CrossOrigin
    @PostMapping("/clearSession")
    public void clearSession(@RequestParam String sessionId) {
        sessionMessages.remove(sessionId);
        logger.debug("Cleared session: {}", sessionId);
    }
} 