package com.sunrise.websocket;

import com.sunrise.core.notifier.SessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class WsEventListener {

    private final SessionRegistry sessionRegistry;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        Long userId = null;

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes(); // userId лежит в атрибутах сессии
        if (sessionAttributes != null) {
            Object userIdObj = sessionAttributes.get("userId");
            if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                try {
                    userId = Long.parseLong((String) userIdObj);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (userId != null && sessionId != null) {
            sessionRegistry.register(sessionId, userId);
            log.info("[🗝️] ✅ WebSocket connected: sessionId={}, userId={}", sessionId, userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = sessionRegistry.getUserId(sessionId);

        if (userId != null) {
            sessionRegistry.unregister(sessionId);
            log.info("[🗝️] ❌ WebSocket disconnected: sessionId={}, userId={}", sessionId, userId);
        }
    }
}
