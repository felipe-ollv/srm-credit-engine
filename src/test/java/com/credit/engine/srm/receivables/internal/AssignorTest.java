package com.credit.engine.srm.receivables.internal;

import com.credit.engine.srm.shared.AssignorId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignorTest {

    @Test
    void shouldCreateAssignorWithNormalizedValues() {
        Assignor assignor = Assignor.create(
                AssignorId.newId(),
                "12.345.678/0001-95",
                "  Indústria Exemplo S.A.  ",
                Instant.parse("2026-08-27T12:00:00Z"));

        assertThat(assignor.document()).isEqualTo("12345678000195");
        assertThat(assignor.legalName()).isEqualTo("Indústria Exemplo S.A.");
    }

    @Test
    void shouldRejectBlankOrOversizedLegalName() {
        assertThatThrownBy(() -> Assignor.create(
                AssignorId.newId(), "12345678000195", " ", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legalName");
    }
}
