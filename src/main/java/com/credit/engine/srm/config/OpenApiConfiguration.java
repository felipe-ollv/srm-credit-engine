package com.credit.engine.srm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

	@Bean
	OpenAPI creditEngineOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("SRM Credit Engine API")
						.description("API para precificação e liquidação auditável de recebíveis")
						.version("v1")
						.contact(new Contact().name("SRM Credit Engine")))
				.components(new Components().addSecuritySchemes(
						"bearerAuth",
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
	}

}
