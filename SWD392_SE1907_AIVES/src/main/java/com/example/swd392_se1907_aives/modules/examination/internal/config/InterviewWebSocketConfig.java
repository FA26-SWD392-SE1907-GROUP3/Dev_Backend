package com.example.swd392_se1907_aives.modules.examination.internal.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import java.util.Arrays;

@Configuration @EnableWebSocket
public class InterviewWebSocketConfig implements WebSocketConfigurer {
    private final LiveTranscriptionHandler handler;
    private final String[] origins;
    public InterviewWebSocketConfig(LiveTranscriptionHandler handler,@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String origins){this.handler=handler;this.origins=Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);}
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
        // MVC's GET /interviews/{id} must not consume "live" as a UUID before the WebSocket upgrade.
        ((ServletWebSocketHandlerRegistry)registry).setOrder(-1);
        registry.addHandler(handler,"/api/v1/interviews/live").setAllowedOrigins(origins);
    }
}
