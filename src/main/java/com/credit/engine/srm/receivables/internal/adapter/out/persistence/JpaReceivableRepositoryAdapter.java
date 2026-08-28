package com.credit.engine.srm.receivables.internal.adapter.out.persistence;

import com.credit.engine.srm.receivables.ReceivableStatusView;
import com.credit.engine.srm.receivables.ReceivableView;
import com.credit.engine.srm.receivables.internal.Receivable;
import com.credit.engine.srm.receivables.internal.ReceivableStatus;
import com.credit.engine.srm.receivables.internal.application.ReceivableRepository;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.PageResult;
import com.credit.engine.srm.shared.ReceivableId;
import com.credit.engine.srm.shared.SettlementId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
class JpaReceivableRepositoryAdapter implements ReceivableRepository {

    private final SpringDataReceivableRepository repository;

    JpaReceivableRepositoryAdapter(SpringDataReceivableRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReceivableView save(Receivable receivable) {
        ReceivableJpaEntity saved = repository.saveAndFlush(new ReceivableJpaEntity(
                receivable.id().value(),
                receivable.assignorId().value(),
                receivable.type(),
                receivable.faceValue().amount(),
                receivable.dueDate(),
                receivable.registrationDate(),
                receivable.status(),
                receivable.createdAt()));
        return toView(saved);
    }

    @Override
    public Optional<Receivable> findById(ReceivableId receivableId) {
        return repository.findById(receivableId.value()).map(JpaReceivableRepositoryAdapter::toDomain);
    }

    @Override
    public void markSettled(Receivable receivable, SettlementId settlementId, Instant settledAt) {
        ReceivableJpaEntity entity = repository.findById(receivable.id().value()).orElseThrow();
        receivable.markSettled(settlementId, settledAt);
        entity.applySettlement(settlementId.value(), settledAt);
        repository.saveAndFlush(entity);
    }

    @Override
    public PageResult<ReceivableView> search(
            AssignorId assignorId,
            ReceivableStatusView status,
            int page,
            int size) {
        Page<ReceivableJpaEntity> result = repository.search(
                assignorId == null ? null : assignorId.value(),
                status == null ? null : ReceivableStatus.valueOf(status.name()),
                PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id"))));
        return new PageResult<>(
                result.getContent().stream().map(JpaReceivableRepositoryAdapter::toView).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private static ReceivableView toView(ReceivableJpaEntity entity) {
        return new ReceivableView(
                new ReceivableId(entity.id),
                new AssignorId(entity.assignorId),
                entity.type,
                new Money(entity.faceValue, Currency.BRL),
                entity.dueDate,
                entity.registrationDate,
                ReceivableStatusView.valueOf(entity.status.name()),
                entity.createdAt);
    }

    private static Receivable toDomain(ReceivableJpaEntity entity) {
        return Receivable.restore(
                new ReceivableId(entity.id),
                new AssignorId(entity.assignorId),
                entity.type,
                new Money(entity.faceValue, Currency.BRL),
                entity.dueDate,
                entity.registrationDate,
                entity.createdAt,
                entity.status,
                entity.settlementId == null ? null : new SettlementId(entity.settlementId),
                entity.settledAt);
    }
}
