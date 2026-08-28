package com.credit.engine.srm.receivables.internal.adapter.out.persistence;

import com.credit.engine.srm.receivables.AssignorView;
import com.credit.engine.srm.receivables.internal.Assignor;
import com.credit.engine.srm.receivables.internal.application.AssignorRepository;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaAssignorRepositoryAdapter implements AssignorRepository {

    private final SpringDataAssignorRepository repository;

    JpaAssignorRepositoryAdapter(SpringDataAssignorRepository repository) {
        this.repository = repository;
    }

    @Override
    public AssignorView save(Assignor assignor) {
        AssignorJpaEntity saved = repository.saveAndFlush(new AssignorJpaEntity(
                assignor.id().value(),
                assignor.document(),
                assignor.legalName(),
                assignor.createdAt()));
        return toView(saved);
    }

    @Override
    public boolean existsByDocument(String document) {
        return repository.existsByDocument(document);
    }

    @Override
    public boolean existsById(AssignorId id) {
        return repository.existsById(id.value());
    }

    @Override
    public Optional<AssignorView> findById(AssignorId id) {
        return repository.findById(id.value()).map(JpaAssignorRepositoryAdapter::toView);
    }

    @Override
    public PageResult<AssignorView> search(String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page, size, Sort.by("legalName").ascending().and(Sort.by("id")));
        Page<AssignorJpaEntity> result = query == null
                ? repository.findAll(pageRequest)
                : repository.search(query, pageRequest);
        return new PageResult<>(
                result.getContent().stream().map(JpaAssignorRepositoryAdapter::toView).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private static AssignorView toView(AssignorJpaEntity entity) {
        return new AssignorView(
                new AssignorId(entity.id), entity.document, entity.legalName, entity.createdAt);
    }
}
