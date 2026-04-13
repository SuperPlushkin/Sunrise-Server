package com.sunrise.config.jwt;

import com.sunrise.core.dataservice.DataOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final DataOrchestrator dataOrchestrator;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        String token = null;

        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
        }

        if (token != null && jwtUtil.validateToken(token)) {
            long userId = jwtUtil.extractUserId(token);
            Integer tokenVersion = jwtUtil.extractJwtVersion(token); // новый метод
            try {
                Optional<Integer> version = dataOrchestrator.getUserJwtVersion(userId);
                if (tokenVersion != null && version.isPresent() && tokenVersion.equals(version.get())) {
                    String sessionId = UUID.randomUUID().toString();
                    attributes.put("userId", userId);
                    attributes.put("sessionId", sessionId);
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}
}
