package com.sunrise.websocket;

import com.sunrise.core.dataservice.DataOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsSubscriptionInterceptor implements ChannelInterceptor {

    private final DataOrchestrator dataOrchestrator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Principal principal = accessor.getUser();
            if (principal == null || principal.getName() == null) {
                log.warn("Subscription denied: no principal");
                return createErrorMessage(accessor, "Not authenticated");
            }
            long userId = Long.parseLong(principal.getName());

            // Expect destination like /topic/chat/123
            if (destination != null && destination.startsWith("/topic/chat/")) {
                try {
                    long chatId = Long.parseLong(destination.substring("/topic/chat/".length()));
                    if (!dataOrchestrator.hasActiveChatMember(userId, chatId)) {
                        log.warn("User {} tried to subscribe to chat {} without membership", userId, chatId);
                        return createErrorMessage(accessor, "Access denied to this chat");
                    }
                } catch (NumberFormatException e) {
                    return createErrorMessage(accessor, "Invalid chat id");
                }
            }
        }
        return message;
    }

    private Message<?> createErrorMessage(StompHeaderAccessor accessor, String errorMessage) {
        StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        errorAccessor.setMessage(errorMessage);
        errorAccessor.setSessionId(accessor.getSessionId());
        return MessageBuilder.createMessage(new byte[0], errorAccessor.getMessageHeaders());
    }
}
