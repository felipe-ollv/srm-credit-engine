package com.credit.engine.srm;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class SrmApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private OpenAPI openApi;

	@Test
	void contextLoadsWithPostgreSqlFlywayAndOpenApi() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where success",
				Integer.class
		);

		assertThat(appliedMigrations).isOne();
		assertThat(openApi.getInfo().getTitle()).isEqualTo("SRM Credit Engine API");
	}

}
