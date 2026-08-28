package com.credit.engine.srm.reporting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementSortTest {

    @Test
    void shouldParseAllowedFieldsAndDirections() {
        assertThat(SettlementSort.parse("settledAt,desc"))
                .isEqualTo(new SettlementSort(
                        SettlementSort.Field.SETTLED_AT,
                        SettlementSort.Direction.DESC));
        assertThat(SettlementSort.parse("assignorLegalName,ASC"))
                .isEqualTo(new SettlementSort(
                        SettlementSort.Field.ASSIGNOR_LEGAL_NAME,
                        SettlementSort.Direction.ASC));
        assertThat(SettlementSort.parse("paymentAmount,asc"))
                .isEqualTo(new SettlementSort(
                        SettlementSort.Field.PAYMENT_AMOUNT,
                        SettlementSort.Direction.ASC));
    }

    @Test
    void shouldRejectAnythingOutsideTheAllowList() {
        assertThatThrownBy(() -> SettlementSort.parse("settledAt;drop table settlements"))
                .isInstanceOf(InvalidSettlementSearchException.class);
        assertThatThrownBy(() -> SettlementSort.parse("paymentAmount,sideways"))
                .isInstanceOf(InvalidSettlementSearchException.class)
                .hasMessageContaining("supports");
    }
}
