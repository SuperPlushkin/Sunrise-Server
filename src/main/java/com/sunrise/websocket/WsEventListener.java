package com.sunrise.websocket;

import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class WsEventListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final DataOrchestrator dataOrchestrator;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            long userId = Long.parseLong(principal.getName());
            presenceService.setStatus(userId, "online");
            broadcastPresenceToUserChats(userId, "online");
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            long userId = Long.parseLong(principal.getName());
            presenceService.setStatus(userId, "offline");
            broadcastPresenceToUserChats(userId, "offline");
            presenceService.removeUserCompletely(userId);
        }
    }

    private void broadcastPresenceToUserChats(long userId, String status) {
        // Get all chat IDs where user is an active member
        Set<Long> chatIds = dataOrchestrator.getActiveChatIdsForUser(userId);
        var payload = new WsRequests.PresenceResponse(userId, status);
        for (Long chatId : chatIds) {
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, payload);
        }
    }
}
