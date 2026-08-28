package com.credit.engine.srm.settlements.internal.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataIdempotencyRepository extends JpaRepository<IdempotencyJpaEntity, String> {
}
