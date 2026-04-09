package com.sunrise.config.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WsUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(WsCurrentUserId.class) &&
                parameter.getParameterType().equals(long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Message<?> message) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
            Object userIdObj = sessionAttributes.get("userId");
            if (userIdObj instanceof Long) {
                return userIdObj;
            }
            if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
        }

        throw new SecurityException("User not authenticated - userId not found in session");
    }
}