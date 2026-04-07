package com.sunrise.config;

import com.sunrise.config.jwt.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final UserIdHandshakeHandler userIdHandshakeHandler;
    private final WsUserIdArgumentResolver wsUserIdArgumentResolver;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor, UserIdHandshakeHandler userIdHandshakeHandler, WsUserIdArgumentResolver wsUserIdArgumentResolver) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.userIdHandshakeHandler = userIdHandshakeHandler;
        this.wsUserIdArgumentResolver = wsUserIdArgumentResolver;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(userIdHandshakeHandler)
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(wsUserIdArgumentResolver);
    }
}
