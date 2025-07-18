package com.cringebook.app.config;

import com.cringebook.app.controllers.Authentication;
import com.cringebook.app.controllers.VideoCallController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private VideoCallController videoCallController;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(videoCallController, "/video-call-ws")
                .setAllowedOrigins("*")
                .addInterceptors(new AuthenticationInterceptor());
    }

    private static class AuthenticationInterceptor implements HandshakeInterceptor {
        private final Authentication authService = new Authentication();

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                     WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            // Extract JWT token from query parameters
            String query = request.getURI().getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        String token = param.substring(6);
                        Integer userId = authService.getIdFromToken(token);
                        if (userId != 0) {
                            attributes.put("userId", userId.toString());
                            return true;
                        }
                    }
                }
            }
            return false; // Reject connection if no valid token
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Exception exception) {
            // Nothing to do after handshake
        }
    }
}