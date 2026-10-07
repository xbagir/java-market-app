package ru.yandex.practicum.mymarket.config;

import io.netty.channel.ChannelOption;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;
import ru.yandex.practicum.mymarket.payment.ApiClient;
import ru.yandex.practicum.mymarket.payment.api.AccountsApi;
import ru.yandex.practicum.mymarket.payment.api.PaymentsApi;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentClientConfig {

    @Bean
    public ApiClient paymentApiClient(WebClient.Builder builder, PaymentProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.timeout().toMillis())
                .responseTimeout(properties.timeout());
        WebClient webClient = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        ApiClient apiClient = new ApiClient(webClient);
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
