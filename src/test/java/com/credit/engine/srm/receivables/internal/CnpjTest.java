package com.credit.engine.srm.receivables.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CnpjTest {

    @Test
    void shouldNormalizeFormattedValidCnpj() {
        assertThat(Cnpj.of("12.345.678/0001-95").value()).isEqualTo("12345678000195");
    }

    @Test
    void shouldRejectInvalidCheckDigitsAndRepeatedDigits() {
        assertThatThrownBy(() -> Cnpj.of("12.345.678/0001-00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid CNPJ");
        assertThatThrownBy(() -> Cnpj.of("11.111.111/1111-11"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid CNPJ");
        assertThatThrownBy(() -> Cnpj.of("1.2.3.4.5.6.7.8.0.0.0.1.9.5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid CNPJ");
    }
}
