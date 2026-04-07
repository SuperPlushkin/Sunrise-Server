package com.sunrise.config;

import com.sunrise.config.annotation.CurrentWsUserId;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WsUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentWsUserId.class) &&
                parameter.getParameterType().equals(long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Message<?> message) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        Principal principal = accessor.getUser();

        if (principal != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid user ID format");
            }
        }
        throw new SecurityException("User not authenticated");
    }
}