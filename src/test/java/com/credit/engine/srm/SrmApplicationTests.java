package com.credit.engine.srm;

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
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
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

	@Test
	void contextLoadsWithPostgreSqlFlywayAndOpenApi() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where success",
				Integer.class
		);

		assertThat(appliedMigrations).isEqualTo(2);
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
					.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));

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
				.andExpect(jsonPath("$.paths['/api/v1/pricing/simulations'].post.responses['503']").exists())
				.andExpect(jsonPath("$.components.schemas.ApiProblem.properties.correlationId").exists())
				.andExpect(jsonPath("$.components.schemas.ApiProblem.properties.fieldErrors").exists())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
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

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
		}
	}

}
