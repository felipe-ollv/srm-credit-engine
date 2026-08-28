package com.credit.engine.srm.currency.internal.adapter.out.http;

import com.credit.engine.srm.currency.internal.application.CurrencyProperties;
import com.credit.engine.srm.currency.internal.application.ExchangeRateProviderUnavailableException;
import com.credit.engine.srm.shared.Currency;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpExchangeRateProviderTest {

    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Test
    void shouldRetryTwiceAndReturnValidSnapshotOnThirdAttempt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(times(2), requestTo("http://provider/rates/USD/BRL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("http://provider/rates/USD/BRL"))
                .andRespond(withSuccess("""
                        {
                          "baseCurrency": "USD",
                          "quoteCurrency": "BRL",
                          "rate": "5.4321",
                          "effectiveAt": "2026-08-27T11:59:00Z"
                        }
                        """, org.springframework.http.MediaType.APPLICATION_JSON));
        List<Duration> pauses = new ArrayList<>();
        HttpExchangeRateProvider provider = provider(builder, pauses::add);

        var result = provider.fetchUsdToBrl();

        assertThat(result.baseCurrency()).isEqualTo(Currency.USD);
        assertThat(result.quoteCurrency()).isEqualTo(Currency.BRL);
        assertThat(result.rate().toPlainString()).isEqualTo("5.4321");
        assertThat(result.capturedAt()).isEqualTo(NOW);
        assertThat(pauses).containsExactly(Duration.ofMillis(250), Duration.ofMillis(250));
        server.verify();
    }

    @Test
    void shouldFailAfterThreeProviderErrors() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(times(3), requestTo("http://provider/rates/USD/BRL"))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
        List<Duration> pauses = new ArrayList<>();
        HttpExchangeRateProvider provider = provider(builder, pauses::add);

        assertThatThrownBy(provider::fetchUsdToBrl)
                .isInstanceOf(ExchangeRateProviderUnavailableException.class)
                .hasMessage("Exchange rate provider is unavailable after 3 attempts");
        assertThat(pauses).hasSize(2);
        server.verify();
    }

    @Test
    void shouldRejectNumericRateBecauseProviderContractRequiresDecimalText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(times(3), requestTo("http://provider/rates/USD/BRL"))
                .andRespond(withSuccess("""
                        {
                          "baseCurrency": "USD",
                          "quoteCurrency": "BRL",
                          "rate": 5.4321,
                          "effectiveAt": "2026-08-27T11:59:00Z"
                        }
                        """, org.springframework.http.MediaType.APPLICATION_JSON));
        HttpExchangeRateProvider provider = provider(builder, duration -> { });

        assertThatThrownBy(provider::fetchUsdToBrl)
                .isInstanceOf(ExchangeRateProviderUnavailableException.class);
        server.verify();
    }

    @Test
    void shouldApplyReadTimeoutToEveryAttempt() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        byte[] responseBody = "{}".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            server.setExecutor(executor);
            server.createContext("/rates/USD/BRL", exchange -> {
                requests.incrementAndGet();
                try {
                    exchange.sendResponseHeaders(200, responseBody.length);
                    Thread.sleep(750);
                    exchange.getResponseBody().write(responseBody);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            server.start();

            CurrencyProperties properties = new CurrencyProperties(
                    "http://localhost:" + server.getAddress().getPort(),
                    Duration.ofMillis(100),
                    Duration.ofMillis(250),
                    3,
                    Duration.ofMillis(1),
                    Duration.ofHours(24),
                    false);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(properties.connectTimeout())
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
            requestFactory.setReadTimeout(properties.readTimeout());
            HttpExchangeRateProvider provider = new HttpExchangeRateProvider(
                    RestClient.builder()
                            .baseUrl(properties.providerBaseUrl())
                            .requestFactory(requestFactory)
                            .build(),
                    properties,
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    duration -> { });

            assertThatThrownBy(provider::fetchUsdToBrl)
                    .isInstanceOf(ExchangeRateProviderUnavailableException.class)
                    .hasMessage("Exchange rate provider is unavailable after 3 attempts");
            assertThat(requests).hasValue(3);
        } finally {
            server.stop(0);
        }
    }

    private static HttpExchangeRateProvider provider(
            RestClient.Builder builder,
            RetrySleeper sleeper) {
        CurrencyProperties properties = new CurrencyProperties(
                "http://provider",
                Duration.ofMillis(500),
                Duration.ofMillis(500),
                3,
                Duration.ofMillis(250),
                Duration.ofHours(24),
                false);
        return new HttpExchangeRateProvider(
                builder.baseUrl(properties.providerBaseUrl()).build(),
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                sleeper);
    }
}
