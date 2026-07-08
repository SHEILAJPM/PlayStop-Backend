package com.playstop.backend.config;

import com.playstop.backend.security.WsAuthHandshakeHandler;
import com.playstop.backend.security.WsSubscriptionAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WsAuthHandshakeHandler wsAuthHandshakeHandler;
    private final WsSubscriptionAuthorizationInterceptor wsSubscriptionAuthorizationInterceptor;

    // Origenes extra solo para desarrollo. Vacio en produccion: ver
    // application.properties vs application-local.properties.
    @Value("${app.cors.dev-origins:}")
    private String corsDevOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = new ArrayList<>(List.of("https://playstop-frontend.onrender.com"));
        if (!corsDevOrigins.isBlank()) {
            for (String origin : corsDevOrigins.split(",")) {
                origins.add(origin.trim());
            }
        }

        registry.addEndpoint("/ws")
                .setHandshakeHandler(wsAuthHandshakeHandler)
                .setAllowedOriginPatterns(origins.toArray(new String[0]))
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(wsSubscriptionAuthorizationInterceptor);
    }
}
