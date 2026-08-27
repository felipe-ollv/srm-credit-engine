package com.credit.engine.srm.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void shouldMapRealmRolesToSpringAuthorities() {
        Instant now = Instant.parse("2026-08-26T19:00:00Z");
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("operator-id")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of("operator", "ADMIN")))
                .build();

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_OPERATOR", "ROLE_ADMIN");
    }

    @Test
    void shouldReturnNoAuthoritiesForMissingOrMalformedClaim() {
        Instant now = Instant.parse("2026-08-26T19:00:00Z");
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-id")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
