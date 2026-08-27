package com.credit.engine.srm.receivables.internal.adapter.out.persistence;

import com.credit.engine.srm.receivables.internal.ReceivableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataReceivableRepository extends JpaRepository<ReceivableJpaEntity, UUID> {

    @Query("""
            select receivable from ReceivableJpaEntity receivable
            where (:assignorId is null or receivable.assignorId = :assignorId)
              and (:status is null or receivable.status = :status)
            """)
    Page<ReceivableJpaEntity> search(
            @Param("assignorId") UUID assignorId,
            @Param("status") ReceivableStatus status,
            Pageable pageable);
}
