package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.payments")
public record PaymentProperties(String baseUrl, Long accountId, Duration timeout) {

    public PaymentProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(5);
        }
    }
}
