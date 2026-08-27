package com.credit.engine.srm.receivables.internal.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataAssignorRepository extends JpaRepository<AssignorJpaEntity, UUID> {

    boolean existsByDocument(String document);

    @Query("""
            select assignor from AssignorJpaEntity assignor
            where :query is null
               or lower(assignor.legalName) like lower(concat('%', :query, '%'))
               or assignor.document like concat('%', :query, '%')
            """)
    Page<AssignorJpaEntity> search(@Param("query") String query, Pageable pageable);
}
