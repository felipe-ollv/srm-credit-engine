package com.credit.engine.srm;

import com.credit.engine.srm.currency.internal.application.ExchangeRateProvider;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(SrmApplicationTests.FixedClockConfiguration.class)
class SrmApplicationTests {

	private static final Instant FIXED_NOW = Instant.parse("2026-08-26T19:00:00Z");
	private static final AtomicInteger DOCUMENT_SEQUENCE = new AtomicInteger();

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private OpenAPI openApi;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void seedCurrentExchangeRate() {
		jdbcTemplate.update("delete from exchange_rates");
		jdbcTemplate.update("""
				insert into exchange_rates
				    (id, base_currency, quote_currency, rate, effective_at, captured_at)
				values (?, 'USD', 'BRL', 5.4321, ?, ?)
				""",
				java.util.UUID.randomUUID(), asUtc(FIXED_NOW), asUtc(FIXED_NOW));
	}

	@Test
	void contextLoadsWithPostgreSqlFlywayAndOpenApi() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where success",
				Integer.class
		);

		assertThat(appliedMigrations).isEqualTo(5);
		assertThat(openApi.getInfo().getTitle()).isEqualTo("SRM Credit Engine API");
		assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
	}

	@Test
	void shouldCreateAndSearchAssignorsAndReceivables() throws Exception {
		mockMvc.perform(post("/api/v1/assignors")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "document": "12.345.678/0001-95",
							  "legalName": "Indústria Exemplo S.A."
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/v1/assignors/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.document").value("12345678000195"))
				.andExpect(jsonPath("$.legalName").value("Indústria Exemplo S.A."));

		java.util.UUID assignorId = jdbcTemplate.queryForObject(
				"select id from assignors where document = ?",
				java.util.UUID.class,
				"12345678000195");

		mockMvc.perform(post("/api/v1/receivables")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "assignorId": "%s",
							  "type": "DUPLICATA_MERCANTIL",
							  "faceValue": "100000.00",
							  "dueDate": "2026-11-26"
							}
							""".formatted(assignorId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.assignorId").value(assignorId.toString()))
				.andExpect(jsonPath("$.faceValue.amount").value("100000.00"))
				.andExpect(jsonPath("$.faceValue.currency").value("BRL"))
				.andExpect(jsonPath("$.status").value("AVAILABLE"));

		mockMvc.perform(get("/api/v1/assignors")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("query", "Exemplo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].document").value("12345678000195"))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/v1/assignors")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].document", hasItem("12345678000195")));

		mockMvc.perform(get("/api/v1/receivables")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("assignorId", assignorId.toString())
					.param("status", "AVAILABLE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].type").value("DUPLICATA_MERCANTIL"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void shouldRejectDuplicateAssignorAndInvalidReceivable() throws Exception {
		String assignor = """
				{
				  "document": "11.222.333/0001-81",
				  "legalName": "Cedente Duplicado Ltda."
				}
				""";
		mockMvc.perform(post("/api/v1/assignors")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(assignor))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/assignors")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(assignor))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ASSIGNOR_DOCUMENT_ALREADY_EXISTS"));

		mockMvc.perform(post("/api/v1/receivables")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "assignorId": "00000000-0000-0000-0000-000000000001",
							  "type": "CHEQUE_PRE_DATADO",
							  "faceValue": "100.00",
							  "dueDate": "2026-11-26"
							}
							"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ASSIGNOR_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/assignors")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "document": "12.345.678/0001-00",
							  "legalName": "CNPJ Inválido Ltda."
							}
							"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("RECEIVABLE_RULE_VIOLATION"));
	}

	@Test
	void shouldProtectAndValidateReceivablesEndpoints() throws Exception {
		mockMvc.perform(get("/api/v1/assignors"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/assignors").with(jwt()))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/receivables")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));

		mockMvc.perform(post("/api/v1/receivables")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "assignorId": "00000000-0000-0000-0000-000000000001",
							  "type": "CHEQUE_PRE_DATADO",
							  "faceValue": 100.00,
							  "dueDate": "2026-11-26"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));
	}

	@Test
	void shouldMatchGoldenCaseC1ThroughHttp() throws Exception {
		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("DUPLICATA_MERCANTIL", "100000.00", "2026-11-26", "BRL")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.presentValue.amount").value("92859.94"))
				.andExpect(jsonPath("$.discount.amount").value("7140.06"))
				.andExpect(jsonPath("$.payment.amount").value("92859.94"))
				.andExpect(jsonPath("$.payment.currency").value("BRL"))
				.andExpect(jsonPath("$.exchangeRate").value(nullValue()))
				.andExpect(jsonPath("$.termMonths").value(3))
				.andExpect(jsonPath("$.baseRate").value("0.01"))
				.andExpect(jsonPath("$.spread").value("0.015"))
				.andExpect(jsonPath("$.pricingDate").value("2026-08-26"))
				.andExpect(jsonPath("$.calculatedAt").value(FIXED_NOW.toString()));
	}

	@Test
	void shouldMatchGoldenCaseC2ThroughHttp() throws Exception {
		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("CHEQUE_PRE_DATADO", "25000.00", "2026-10-26", "BRL")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.presentValue.amount").value("23337.77"))
				.andExpect(jsonPath("$.discount.amount").value("1662.23"))
				.andExpect(jsonPath("$.spread").value("0.025"));
	}

	@Test
	void shouldMatchGoldenCaseC3ThroughHttp() throws Exception {
		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("DUPLICATA_MERCANTIL", "100000.00", "2026-11-26", "USD")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.presentValue.amount").value("92859.94"))
				.andExpect(jsonPath("$.payment.amount").value("17094.67"))
				.andExpect(jsonPath("$.payment.currency").value("USD"))
				.andExpect(jsonPath("$.discount.amount").value("7140.06"))
				.andExpect(jsonPath("$.exchangeRate.rate").value("5.4321"))
				.andExpect(jsonPath("$.exchangeRate.effectiveAt").value(FIXED_NOW.toString()));
	}

	@Test
	void shouldQueryAndRefreshPersistedExchangeRatesWithRequiredRoles() throws Exception {
		jdbcTemplate.update("""
				insert into exchange_rates
				    (id, base_currency, quote_currency, rate, effective_at, captured_at)
				values (?, 'USD', 'BRL', 9.9999, ?, ?)
				""",
				java.util.UUID.randomUUID(),
				asUtc(FIXED_NOW.plusSeconds(60)),
				asUtc(FIXED_NOW.plusSeconds(60)));

		mockMvc.perform(get("/api/v1/exchange-rates/current")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("baseCurrency", "USD")
					.param("quoteCurrency", "BRL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value("5.4321"))
				.andExpect(jsonPath("$.effectiveAt").value(FIXED_NOW.toString()));

		mockMvc.perform(post("/api/v1/exchange-rates/refresh")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/exchange-rates/refresh")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rate").value("5.5"));

		Integer snapshots = jdbcTemplate.queryForObject(
				"select count(*) from exchange_rates",
				Integer.class);
		assertThat(snapshots).isEqualTo(3);
	}

	@Test
	void shouldReturnNotFoundForExpiredRateAndRequireAuthentication() throws Exception {
		jdbcTemplate.update("delete from exchange_rates");
		Instant expired = FIXED_NOW.minusSeconds(24 * 60 * 60 + 1);
		jdbcTemplate.update("""
				insert into exchange_rates
				    (id, base_currency, quote_currency, rate, effective_at, captured_at)
				values (?, 'USD', 'BRL', 5.4321, ?, ?)
				""",
				java.util.UUID.randomUUID(), asUtc(expired), asUtc(expired));

		mockMvc.perform(get("/api/v1/exchange-rates/current")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("baseCurrency", "USD")
					.param("quoteCurrency", "BRL"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("FX_RATE_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/exchange-rates/refresh"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/exchange-rates/current")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("baseCurrency", "BRL")
					.param("quoteCurrency", "BRL"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));
	}

	@Test
	void shouldRequireAuthenticationAndRequiredRole() throws Exception {
		String body = request("DUPLICATA_MERCANTIL", "100000.00", "2026-11-26", "BRL");

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
				.andExpect(jsonPath("$.correlationId").isNotEmpty())
				.andExpect(header().string("WWW-Authenticate", "Bearer"));

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt())
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void shouldReturnProblemDetailsForInvalidRequestAndDomainRule() throws Exception {
		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "receivableType": "DUPLICATA_MERCANTIL",
							  "faceValue": 100000.00,
							  "dueDate": "2026-11-26",
							  "paymentCurrency": "BRL"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("X-Correlation-Id", "front-request-123")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("DUPLICATA_MERCANTIL", "1e3", "2026-11-26", "BRL")))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"))
				.andExpect(jsonPath("$.fieldErrors.faceValue").isNotEmpty())
				.andExpect(jsonPath("$.correlationId").value("front-request-123"))
				.andExpect(header().string("X-Correlation-Id", "front-request-123"));

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("TIPO_INEXISTENTE", "100000.00", "2026-11-26", "BRL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("DUPLICATA_MERCANTIL", "100.001", "2026-11-26", "BRL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.faceValue").isNotEmpty());

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("DUPLICATA_MERCANTIL", "100000.00", "2026-08-26", "BRL")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("PRICING_RULE_VIOLATION"));

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("DUPLICATA_MERCANTIL", "100000.00", "2056-08-27", "BRL")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("PRICING_RULE_VIOLATION"));
	}

	@Test
	void shouldApplyCorsAllowlistAndGenerateCorrelationId() throws Exception {
		mockMvc.perform(options("/api/v1/pricing/simulations")
					.header("Origin", "http://localhost:4200")
					.header("Access-Control-Request-Method", "POST")
					.header("Access-Control-Request-Headers", "authorization,idempotency-key"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
				.andExpect(header().string(
						"Access-Control-Allow-Headers",
						org.hamcrest.Matchers.containsStringIgnoringCase("Idempotency-Key")));

		mockMvc.perform(options("/api/v1/pricing/simulations")
					.header("Origin", "https://not-allowed.example")
					.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

		mockMvc.perform(post("/api/v1/pricing/simulations")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("X-Correlation-Id", "invalid value with spaces")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request("DUPLICATA_MERCANTIL", "100000.00", "2026-11-26", "BRL")))
				.andExpect(status().isOk())
				.andExpect(header().string(
						"X-Correlation-Id",
						matchesPattern("[0-9a-f-]{36}")));
	}

	@Test
	void shouldExposeProtectedOpenApiContract() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/actuator/prometheus"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/actuator/prometheus")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/actuator/prometheus")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("# HELP")));

		mockMvc.perform(get("/actuator"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/v3/api-docs")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/pricing/simulations'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/assignors'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/assignors'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/receivables'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/receivables'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/exchange-rates/refresh'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/exchange-rates/current'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/settlement-batches'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/settlements'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/pricing/simulations'].post.responses['503']").exists())
				.andExpect(jsonPath("$.components.schemas.ApiProblem.properties.correlationId").exists())
				.andExpect(jsonPath("$.components.schemas.ApiProblem.properties.fieldErrors").exists())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
	}

	@Test
	void shouldSettleMixedBatchAndReplayPersistedResponse() throws Exception {
		UUID brlReceivable = createReceivable("DUPLICATA_MERCANTIL", "100000.00", LocalDate.parse("2026-11-26"));
		UUID usdReceivable = createReceivable("DUPLICATA_MERCANTIL", "100000.00", LocalDate.parse("2026-11-26"));
		String body = settlementRequest(brlReceivable, "BRL", usdReceivable, "USD");

		String original = mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "mixed-batch-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.items[0].status").value("SUCCESS"))
				.andExpect(jsonPath("$.items[0].settlement.assignorLegalName")
						.value("Settlement Test Assignor"))
				.andExpect(jsonPath("$.items[0].settlement.presentValue.amount").value("92859.94"))
				.andExpect(jsonPath("$.items[0].settlement.exchangeRate").value(nullValue()))
				.andExpect(jsonPath("$.items[1].status").value("SUCCESS"))
				.andExpect(jsonPath("$.items[1].settlement.payment.amount").value("17094.67"))
				.andExpect(jsonPath("$.items[1].settlement.exchangeRate.rate").value("5.4321"))
				.andReturn().getResponse().getContentAsString();

		String replay = mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
					.header("Idempotency-Key", "mixed-batch-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(replay).isEqualTo(original);
		assertThat(countSettlements(brlReceivable, usdReceivable)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from receivables where id in (?, ?) and status = 'SETTLED'",
				Integer.class, brlReceivable, usdReceivable)).isEqualTo(2);
	}

	@Test
	void shouldRejectChangedPayloadAndInvalidEnvelopeWithoutEffects() throws Exception {
		UUID first = createReceivable("CHEQUE_PRE_DATADO", "25000.00", LocalDate.parse("2026-10-26"));
		UUID second = createReceivable("CHEQUE_PRE_DATADO", "25000.00", LocalDate.parse("2026-10-26"));

		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "payload-conflict-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(first, "BRL")))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "payload-conflict-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(second, "BRL")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IDEMPOTENCY_PAYLOAD_CONFLICT"));

		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "duplicate-envelope-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(second, "BRL", second, "USD")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("SETTLEMENT_BATCH_INVALID"));

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from settlement_idempotency where idempotency_key = 'duplicate-envelope-001'",
				Integer.class)).isZero();
	}

	@Test
	void shouldKeepBrlSuccessWhenUsdRateIsUnavailable() throws Exception {
		jdbcTemplate.update("delete from exchange_rates");
		UUID brlReceivable = createReceivable("DUPLICATA_MERCANTIL", "100000.00", LocalDate.parse("2026-11-26"));
		UUID usdReceivable = createReceivable("DUPLICATA_MERCANTIL", "100000.00", LocalDate.parse("2026-11-26"));

		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "partial-fx-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(brlReceivable, "BRL", usdReceivable, "USD")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].status").value("SUCCESS"))
				.andExpect(jsonPath("$.items[1].status").value("FX_RATE_UNAVAILABLE"))
				.andExpect(jsonPath("$.items[1].code").value("FX_RATE_UNAVAILABLE"));

		assertThat(countSettlements(brlReceivable, usdReceivable)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select status from receivables where id = ?",
				String.class, usdReceivable)).isEqualTo("AVAILABLE");
	}

	@Test
	void shouldAllowExactlyOneConcurrentSettlement() throws Exception {
		UUID receivable = createReceivable("CHEQUE_PRE_DATADO", "25000.00", LocalDate.parse("2026-10-26"));
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Future<String> first = executor.submit(() -> concurrentSettlement(start, receivable, "concurrent-a"));
			Future<String> second = executor.submit(() -> concurrentSettlement(start, receivable, "concurrent-b"));
			start.countDown();
			String firstResponse = first.get();
			String secondResponse = second.get();

			assertThat(List.of(firstResponse, secondResponse))
					.filteredOn(response -> response.contains("\"status\":\"SUCCESS\""))
					.hasSize(1);
			assertThat(List.of(firstResponse, secondResponse))
					.filteredOn(response -> response.contains("\"status\":\"CONFLICT\""))
					.hasSize(1);
		}
		assertThat(countSettlements(receivable)).isEqualTo(1);
	}

	@Test
	void shouldProtectSettlementBatchAndRejectInvalidKey() throws Exception {
		UUID receivable = createReceivable("DUPLICATA_MERCANTIL", "1000.00", LocalDate.parse("2026-11-26"));
		mockMvc.perform(post("/api/v1/settlement-batches")
					.header("Idempotency-Key", "auth-required")
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(receivable, "BRL")))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(receivable, "BRL")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));

		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "invalid key with spaces")
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(receivable, "BRL")))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void shouldRejectBatchOutsideItemLimitBeforeClaimingIdempotency() throws Exception {
		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "empty-batch-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"items\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));

		String overLimitItems = java.util.stream.IntStream.range(0, 101)
				.mapToObj(ignored -> "{\"receivableId\":\"" + UUID.randomUUID()
						+ "\",\"paymentCurrency\":\"BRL\"}")
				.collect(java.util.stream.Collectors.joining(","));
		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
					.header("Idempotency-Key", "over-limit-batch-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"items\":[" + overLimitItems + "]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));

		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from settlement_idempotency where idempotency_key in (?, ?)",
				Integer.class,
				"empty-batch-001",
				"over-limit-batch-001")).isZero();
	}

	@Test
	void shouldReturnConflictForIdempotencyKeyAlreadyInProgress() throws Exception {
		UUID batchId = UUID.randomUUID();
		UUID receivable = createReceivable("DUPLICATA_MERCANTIL", "1000.00", LocalDate.parse("2026-11-26"));
		String body = settlementRequest(receivable, "BRL");
		String requestHash = java.util.HexFormat.of().formatHex(
				java.security.MessageDigest.getInstance("SHA-256").digest(
						(receivable + ":BRL\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		jdbcTemplate.update("""
				insert into settlement_batches (id, status, requested_at)
				values (?, 'PROCESSING', ?)
				""", batchId, asUtc(FIXED_NOW));
		jdbcTemplate.update("""
				insert into settlement_idempotency
				    (idempotency_key, request_hash, batch_id, status, created_at)
				values ('processing-key-001', ?, ?, 'PROCESSING', ?)
				""", requestHash, batchId, asUtc(FIXED_NOW));

		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", "processing-key-001")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IDEMPOTENCY_IN_PROGRESS"));
	}

	@Test
	void shouldRollbackSettlementWhenReceivableUpdateFails() throws Exception {
		UUID receivable = createReceivable("DUPLICATA_MERCANTIL", "1000.00", LocalDate.parse("2026-11-26"));
		jdbcTemplate.execute("""
				create function reject_receivable_settlement() returns trigger
				language plpgsql as $$
				begin
				    if new.status = 'SETTLED' then
				        raise exception 'forced receivable update failure';
				    end if;
				    return new;
				end
				$$
				""");
		jdbcTemplate.execute("""
				create trigger reject_receivable_settlement_trigger
				before update on receivables
				for each row execute function reject_receivable_settlement()
				""");
		try {
			mockMvc.perform(post("/api/v1/settlement-batches")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
						.header("Idempotency-Key", "rollback-item-001")
						.contentType(MediaType.APPLICATION_JSON)
						.content(settlementRequest(receivable, "BRL")))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.items[0].status").value("TECHNICAL_ERROR"))
					.andExpect(jsonPath("$.items[0].code").value("SETTLEMENT_PERSISTENCE_FAILED"));
		} finally {
			jdbcTemplate.execute("drop trigger reject_receivable_settlement_trigger on receivables");
			jdbcTemplate.execute("drop function reject_receivable_settlement()");
		}

		assertThat(countSettlements(receivable)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"select status from receivables where id = ?", String.class, receivable))
				.isEqualTo("AVAILABLE");
	}

	@Test
	void shouldSearchSettlementStatementsWithCombinedFiltersAndInclusiveBusinessDates()
			throws Exception {
		UUID assignorId = createAssignor("Reporting Assignor Ltda.");
		createReportedSettlement(
				assignorId,
				"DUPLICATA_MERCANTIL",
				"50000.00",
				"BRL",
				"report-filter-brl-before",
				Instant.parse("2026-08-26T20:00:00Z"));
		ReportedSettlement usd = createReportedSettlement(
				assignorId,
				"DUPLICATA_MERCANTIL",
				"100000.00",
				"USD",
				"report-filter-usd",
				Instant.parse("2026-08-27T15:00:00Z"));
		createReportedSettlement(
				assignorId,
				"CHEQUE_PRE_DATADO",
				"25000.00",
				"BRL",
				"report-filter-brl-after",
				Instant.parse("2026-08-28T15:00:00Z"));

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("from", "2026-08-27")
					.param("to", "2026-08-28")
					.param("assignorId", assignorId.toString())
					.param("paymentCurrency", "USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].settlementId")
						.value(usd.settlementId().toString()))
				.andExpect(jsonPath("$.content[0].assignorId").value(assignorId.toString()))
				.andExpect(jsonPath("$.content[0].assignorDocument")
						.value(matchesPattern("[0-9]{14}")))
				.andExpect(jsonPath("$.content[0].assignorLegalName")
						.value("Reporting Assignor Ltda."))
				.andExpect(jsonPath("$.content[0].receivableType")
						.value("DUPLICATA_MERCANTIL"))
				.andExpect(jsonPath("$.content[0].faceValue.amount").value("100000.00"))
				.andExpect(jsonPath("$.content[0].presentValue.amount").value("92859.94"))
				.andExpect(jsonPath("$.content[0].discount.amount").value("7140.06"))
				.andExpect(jsonPath("$.content[0].payment.amount").value("17094.67"))
				.andExpect(jsonPath("$.content[0].payment.currency").value("USD"))
				.andExpect(jsonPath("$.content[0].termMonths").value(3))
				.andExpect(jsonPath("$.content[0].baseRate").value("0.01"))
				.andExpect(jsonPath("$.content[0].spread").value("0.015"))
				.andExpect(jsonPath("$.content[0].exchangeRate.rate").value("5.4321"))
				.andExpect(jsonPath("$.content[0].settledAt")
						.value("2026-08-27T15:00:00Z"));

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
					.param("from", "2026-08-28")
					.param("to", "2026-08-28")
					.param("assignorId", assignorId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].payment.currency").value("BRL"));

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("paymentCurrency", "USD")
					.param("size", "100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].payment.currency", everyItem(equalTo("USD"))));
	}

	@Test
	void shouldPageAndSortSettlementStatementsOnTheServer() throws Exception {
		UUID assignorId = createAssignor("Paged Reporting Ltda.");
		createReportedSettlement(
				assignorId, "DUPLICATA_MERCANTIL", "3000.00", "BRL",
				"report-page-3000", Instant.parse("2026-08-28T15:00:00Z"));
		createReportedSettlement(
				assignorId, "DUPLICATA_MERCANTIL", "1000.00", "BRL",
				"report-page-1000", Instant.parse("2026-08-26T20:00:00Z"));
		createReportedSettlement(
				assignorId, "DUPLICATA_MERCANTIL", "2000.00", "BRL",
				"report-page-2000", Instant.parse("2026-08-27T15:00:00Z"));

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("assignorId", assignorId.toString())
					.param("page", "0")
					.param("size", "2")
					.param("sort", "paymentAmount,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].faceValue.amount").value("1000.00"))
				.andExpect(jsonPath("$.content[1].faceValue.amount").value("2000.00"));

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("assignorId", assignorId.toString())
					.param("page", "1")
					.param("size", "2")
					.param("sort", "paymentAmount,asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].faceValue.amount").value("3000.00"));
	}

	@Test
	void shouldProtectReportingAndRejectInvalidFilters() throws Exception {
		mockMvc.perform(get("/api/v1/settlements"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/settlements").with(jwt()))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("from", "2026-08-28")
					.param("to", "2026-08-27"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REPORT_QUERY_INVALID"));

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("sort", "settledAt;drop table settlements"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REPORT_QUERY_INVALID"));

		mockMvc.perform(get("/api/v1/settlements")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_INVALID"));
	}

	@Test
	void shouldUseCombinedReportingIndexForThePrimaryFilteredPlan() throws Exception {
		UUID assignorId = createAssignor("Indexed Reporting Ltda.");
		createReportedSettlement(
				assignorId, "DUPLICATA_MERCANTIL", "100000.00", "USD",
				"report-index-usd", Instant.parse("2026-08-27T15:00:00Z"));

		String plan = jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<String>) connection -> {
			try (java.sql.Statement statement = connection.createStatement()) {
				statement.execute("set enable_seqscan = off");
			}
			try (java.sql.PreparedStatement statement = connection.prepareStatement("""
					explain (costs off)
					select id
					  from settlements
					 where assignor_id = ? and payment_currency = ?
					 order by settled_at desc, id desc
					 limit 20
					""")) {
				statement.setObject(1, assignorId);
				statement.setString(2, "USD");
				try (java.sql.ResultSet resultSet = statement.executeQuery()) {
					StringBuilder lines = new StringBuilder();
					while (resultSet.next()) {
						lines.append(resultSet.getString(1)).append('\n');
					}
					return lines.toString();
				}
			} finally {
				try (java.sql.Statement statement = connection.createStatement()) {
					statement.execute("reset enable_seqscan");
				}
			}
		});

		assertThat(plan).contains("idx_settlements_assignor_currency_period_id");
	}

	private UUID createReceivable(String type, String faceValue, LocalDate dueDate) {
		return createReceivable(createAssignor("Settlement Test Assignor"), type, faceValue, dueDate);
	}

	private UUID createAssignor(String legalName) {
		UUID assignorId = UUID.randomUUID();
		String document = "%014d".formatted(90_000_000L + DOCUMENT_SEQUENCE.incrementAndGet());
		jdbcTemplate.update("""
				insert into assignors (id, document, legal_name, created_at)
				values (?, ?, ?, ?)
				""", assignorId, document, legalName, asUtc(FIXED_NOW));
		return assignorId;
	}

	private UUID createReceivable(
			UUID assignorId,
			String type,
			String faceValue,
			LocalDate dueDate) {
		UUID receivableId = UUID.randomUUID();
		jdbcTemplate.update("""
				insert into receivables
				    (id, assignor_id, type, face_value, due_date, registration_date,
				     status, version, created_at)
				values (?, ?, ?, ?::numeric, ?, ?, 'AVAILABLE', 0, ?)
				""",
				receivableId,
				assignorId,
				type,
				faceValue,
				dueDate,
				LocalDate.parse("2026-08-26"),
				asUtc(FIXED_NOW));
		return receivableId;
	}

	private ReportedSettlement createReportedSettlement(
			UUID assignorId,
			String type,
			String faceValue,
			String paymentCurrency,
			String idempotencyKey,
			Instant settledAt) throws Exception {
		UUID receivableId = createReceivable(
				assignorId, type, faceValue, LocalDate.parse("2026-11-26"));
		mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", idempotencyKey)
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(receivableId, paymentCurrency)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].status").value("SUCCESS"));
		UUID settlementId = jdbcTemplate.queryForObject(
				"select id from settlements where receivable_id = ?",
				UUID.class,
				receivableId);
		LocalDate pricingDate = LocalDate.ofInstant(
				settledAt, java.time.ZoneId.of("America/Sao_Paulo"));
		jdbcTemplate.update("""
				update settlements
				   set pricing_date = ?, calculated_at = ?, settled_at = ?
				 where id = ?
				""", pricingDate, asUtc(settledAt), asUtc(settledAt), settlementId);
		jdbcTemplate.update(
				"update receivables set settled_at = ? where id = ?",
				asUtc(settledAt),
				receivableId);
		return new ReportedSettlement(settlementId, receivableId);
	}

	private record ReportedSettlement(UUID settlementId, UUID receivableId) {
	}

	private int countSettlements(UUID... receivableIds) {
		String placeholders = String.join(",", java.util.Collections.nCopies(receivableIds.length, "?"));
		return jdbcTemplate.queryForObject(
				"select count(*) from settlements where receivable_id in (" + placeholders + ")",
				Integer.class,
				(Object[]) receivableIds);
	}

	private String concurrentSettlement(
			CountDownLatch start,
			UUID receivableId,
			String idempotencyKey) throws Exception {
		start.await();
		return mockMvc.perform(post("/api/v1/settlement-batches")
					.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATOR")))
					.header("Idempotency-Key", idempotencyKey)
					.contentType(MediaType.APPLICATION_JSON)
					.content(settlementRequest(receivableId, "BRL")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private static String settlementRequest(Object... receivableAndCurrencyPairs) {
		StringBuilder items = new StringBuilder();
		for (int index = 0; index < receivableAndCurrencyPairs.length; index += 2) {
			if (!items.isEmpty()) {
				items.append(',');
			}
			items.append("{\"receivableId\":\"")
					.append(receivableAndCurrencyPairs[index])
					.append("\",\"paymentCurrency\":\"")
					.append(receivableAndCurrencyPairs[index + 1])
					.append("\"}");
		}
		return "{\"items\":[" + items + "]}";
	}

	private static String request(
			String receivableType,
			String faceValue,
			String dueDate,
			String paymentCurrency) {

		return """
				{
				  "receivableType": "%s",
				  "faceValue": "%s",
				  "dueDate": "%s",
				  "paymentCurrency": "%s"
				}
				""".formatted(receivableType, faceValue, dueDate, paymentCurrency);
	}

	private static OffsetDateTime asUtc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
		}

		@Bean
		@Primary
		ExchangeRateProvider testExchangeRateProvider() {
			return () -> new ExchangeRate(
					Currency.USD,
					Currency.BRL,
					new BigDecimal("5.5000"),
					FIXED_NOW.minusSeconds(1),
					FIXED_NOW);
		}
	}

}
