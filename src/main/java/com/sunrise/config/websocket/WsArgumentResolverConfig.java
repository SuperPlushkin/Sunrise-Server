package com.sunrise.config.websocket;

import com.sunrise.config.annotation.WsUserIdArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
public class WsArgumentResolverConfig implements WebSocketMessageBrokerConfigurer {

    private final WsUserIdArgumentResolver wsUserIdArgumentResolver;

    public WsArgumentResolverConfig(WsUserIdArgumentResolver wsUserIdArgumentResolver) {
        this.wsUserIdArgumentResolver = wsUserIdArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(wsUserIdArgumentResolver);
    }
}