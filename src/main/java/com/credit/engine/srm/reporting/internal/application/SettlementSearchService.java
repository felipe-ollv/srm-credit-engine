package com.credit.engine.srm.reporting.internal.application;

import com.credit.engine.srm.reporting.InvalidSettlementSearchException;
import com.credit.engine.srm.reporting.SearchSettlementsUseCase;
import com.credit.engine.srm.reporting.SettlementSearchQuery;
import com.credit.engine.srm.reporting.SettlementStatement;
import com.credit.engine.srm.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class SettlementSearchService implements SearchSettlementsUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final SettlementStatementRepository repository;

    SettlementSearchService(SettlementStatementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SettlementStatement> search(SettlementSearchQuery query) {
        validate(query);
        Instant fromInclusive = query.from() == null
                ? null
                : query.from().atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = query.to() == null
                ? null
                : query.to().plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        return repository.search(new SettlementSearchCriteria(
                fromInclusive,
                toExclusive,
                query.assignorId(),
                query.paymentCurrency(),
                query.page(),
                query.size(),
                query.sort()));
    }

    private static void validate(SettlementSearchQuery query) {
        if (query == null) {
            throw new InvalidSettlementSearchException("Search query is required");
        }
        LocalDate from = query.from();
        LocalDate to = query.to();
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidSettlementSearchException("from cannot be after to");
        }
        if (query.page() < 0 || query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new InvalidSettlementSearchException(
                    "page must be non-negative and size must be between 1 and 100");
        }
        if (query.sort() == null) {
            throw new InvalidSettlementSearchException("sort is required");
        }
    }
}
