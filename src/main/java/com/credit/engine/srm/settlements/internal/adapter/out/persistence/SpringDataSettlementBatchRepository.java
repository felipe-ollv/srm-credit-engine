package com.credit.engine.srm.settlements.internal.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataSettlementBatchRepository extends JpaRepository<SettlementBatchJpaEntity, UUID> {
}
