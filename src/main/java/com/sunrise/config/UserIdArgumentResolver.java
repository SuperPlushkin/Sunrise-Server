package com.sunrise.config;

import com.sunrise.config.annotation.CurrentUserId;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

@Component
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class) &&
                parameter.getParameterType().equals(long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new SecurityException("User not authenticated");

        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> detailsMap) {
            Object userIdObj = detailsMap.get("userId");
            if (userIdObj instanceof Long) {
                long userId = (Long) userIdObj;
                if (userId <= 0) throw new IllegalArgumentException("User ID must be positive: " + userId);
                return userId;
            }
        }
        throw new IllegalStateException("User ID not found in JWT token");
    }
}
