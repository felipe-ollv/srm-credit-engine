package com.credit.engine.srm.currency.internal.config;

import com.credit.engine.srm.currency.RefreshExchangeRateUseCase;
import com.credit.engine.srm.currency.internal.adapter.out.http.HttpExchangeRateProvider;
import com.credit.engine.srm.currency.internal.application.CurrencyProperties;
import com.credit.engine.srm.currency.internal.application.ExchangeRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CurrencyProperties.class)
public class CurrencyModuleConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrencyModuleConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(ExchangeRateProvider.class)
    ExchangeRateProvider exchangeRateProvider(CurrencyProperties properties, Clock clock) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.providerBaseUrl())
                .requestFactory(requestFactory)
                .build();
        return new HttpExchangeRateProvider(restClient, properties, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.currency", name = "bootstrap-enabled", havingValue = "true")
    ApplicationRunner bootstrapExchangeRate(RefreshExchangeRateUseCase refreshUseCase) {
        return arguments -> {
            try {
                refreshUseCase.refreshUsdToBrl();
                LOGGER.info("Initial USD/BRL exchange rate captured");
            } catch (RuntimeException exception) {
                LOGGER.warn("Initial USD/BRL exchange rate could not be captured");
            }
        };
    }
}
