package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class StubPaymentConfig {

    @Bean
    @Primary
    public StubPaymentGateway stubPaymentGateway() {
        return new StubPaymentGateway();
    }
}
