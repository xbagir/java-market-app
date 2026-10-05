package ru.yandex.practicum.mymarket.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import ru.yandex.practicum.mymarket.payment.ApiClient;
import ru.yandex.practicum.mymarket.payment.api.AccountsApi;
import ru.yandex.practicum.mymarket.payment.api.PaymentsApi;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentClientConfig {

    @Bean
    public ApiClient paymentApiClient(WebClient.Builder builder, PaymentProperties properties) {
        ApiClient apiClient = new ApiClient(builder.build());
        apiClient.setBasePath(properties.baseUrl());
        return apiClient;
    }

    @Bean
    public AccountsApi accountsApi(ApiClient paymentApiClient) {
        return new AccountsApi(paymentApiClient);
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient paymentApiClient) {
        return new PaymentsApi(paymentApiClient);
    }
}
