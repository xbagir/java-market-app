package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;

@Configuration
public class OAuth2ClientConfig {

    private static final String PAYMENT_CLIENT_REGISTRATION = "payment-service";

    @Bean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.client.registration.payment-service",
            name = "client-id")
    public ServerOAuth2AuthorizedClientExchangeFilterFunction paymentOAuth2Filter(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction filter =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        filter.setDefaultClientRegistrationId(PAYMENT_CLIENT_REGISTRATION);
        return filter;
    }
}
