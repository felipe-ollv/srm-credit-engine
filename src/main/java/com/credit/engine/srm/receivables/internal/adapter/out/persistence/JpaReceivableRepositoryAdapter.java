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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

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
}
