package com.credit.engine.srm.receivables.internal.adapter.out.persistence;

import com.credit.engine.srm.receivables.internal.ReceivableStatus;
import com.credit.engine.srm.shared.ReceivableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "receivables")
class ReceivableJpaEntity {

    @Id
    UUID id;

    @Column(name = "assignor_id", nullable = false)
    UUID assignorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    ReceivableType type;

    @Column(name = "face_value", nullable = false, precision = 19, scale = 2)
    BigDecimal faceValue;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Column(name = "registration_date", nullable = false)
    LocalDate registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ReceivableStatus status;

    @Column(name = "settlement_id")
    UUID settlementId;

    @Column(name = "settled_at")
    Instant settledAt;

    @Version
    @Column(nullable = false)
    long version;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected ReceivableJpaEntity() {
    }

    ReceivableJpaEntity(
            UUID id,
            UUID assignorId,
            ReceivableType type,
            BigDecimal faceValue,
            LocalDate dueDate,
            LocalDate registrationDate,
            ReceivableStatus status,
            Instant createdAt) {
        this.id = id;
        this.assignorId = assignorId;
        this.type = type;
        this.faceValue = faceValue;
        this.dueDate = dueDate;
        this.registrationDate = registrationDate;
        this.status = status;
        this.createdAt = createdAt;
    }
}
