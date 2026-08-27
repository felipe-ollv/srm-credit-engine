package com.credit.engine.srm.receivables.internal.application;

import com.credit.engine.srm.receivables.AssignorView;
import com.credit.engine.srm.receivables.CreateAssignorCommand;
import com.credit.engine.srm.receivables.CreateAssignorUseCase;
import com.credit.engine.srm.receivables.CreateReceivableCommand;
import com.credit.engine.srm.receivables.CreateReceivableUseCase;
import com.credit.engine.srm.receivables.ReceivableView;
import com.credit.engine.srm.receivables.SearchAssignorsQuery;
import com.credit.engine.srm.receivables.SearchAssignorsUseCase;
import com.credit.engine.srm.receivables.SearchReceivablesQuery;
import com.credit.engine.srm.receivables.SearchReceivablesUseCase;
import com.credit.engine.srm.receivables.internal.Assignor;
import com.credit.engine.srm.receivables.internal.Receivable;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.PageResult;
import com.credit.engine.srm.shared.ReceivableId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class ReceivablesService implements
        CreateAssignorUseCase,
        SearchAssignorsUseCase,
        CreateReceivableUseCase,
        SearchReceivablesUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final AssignorRepository assignors;
    private final ReceivableRepository receivables;
    private final Clock clock;
    private final ZoneId businessZone;

    ReceivablesService(
            AssignorRepository assignors,
            ReceivableRepository receivables,
            Clock clock) {
        this.assignors = Objects.requireNonNull(assignors, "assignors is required");
        this.receivables = Objects.requireNonNull(receivables, "receivables is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.businessZone = ZoneId.of("America/Sao_Paulo");
    }

    @Override
    @Transactional
    public AssignorView create(CreateAssignorCommand command) {
        Objects.requireNonNull(command, "command is required");
        Instant now = clock.instant();
        final Assignor assignor;
        try {
            assignor = Assignor.create(
                    AssignorId.newId(), command.document(), command.legalName(), now);
        } catch (IllegalArgumentException exception) {
            throw new ReceivableRuleViolationException(exception.getMessage(), exception);
        }
        if (assignors.existsByDocument(assignor.document())) {
            throw new AssignorDocumentAlreadyExistsException();
        }
        try {
            return assignors.save(assignor);
        } catch (DataIntegrityViolationException exception) {
            throw new AssignorDocumentAlreadyExistsException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssignorView> search(SearchAssignorsQuery query) {
        Objects.requireNonNull(query, "query is required");
        validatePage(query.page(), query.size());
        String normalized = query.query() == null || query.query().isBlank()
                ? null
                : query.query().trim();
        return assignors.search(normalized, query.page(), query.size());
    }

    @Override
    @Transactional
    public ReceivableView create(CreateReceivableCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (!assignors.existsById(command.assignorId())) {
            throw new AssignorNotFoundException();
        }
        Instant now = clock.instant();
        LocalDate registrationDate = LocalDate.ofInstant(now, businessZone);
        try {
            Receivable receivable = Receivable.create(
                    ReceivableId.newId(),
                    command.assignorId(),
                    command.type(),
                    new Money(command.faceValue(), Currency.BRL),
                    command.dueDate(),
                    registrationDate,
                    now);
            return receivables.save(receivable);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new ReceivableRuleViolationException(exception.getMessage(), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReceivableView> search(SearchReceivablesQuery query) {
        Objects.requireNonNull(query, "query is required");
        validatePage(query.page(), query.size());
        return receivables.search(query.assignorId(), query.status(), query.page(), query.size());
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
    }
}
