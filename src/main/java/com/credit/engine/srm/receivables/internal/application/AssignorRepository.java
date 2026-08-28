package com.credit.engine.srm.receivables.internal.application;

import com.credit.engine.srm.receivables.AssignorView;
import com.credit.engine.srm.receivables.internal.Assignor;
import com.credit.engine.srm.shared.AssignorId;
import com.credit.engine.srm.shared.PageResult;

import java.util.Optional;

public interface AssignorRepository {

    AssignorView save(Assignor assignor);

    boolean existsByDocument(String document);

    boolean existsById(AssignorId id);

    Optional<AssignorView> findById(AssignorId id);

    PageResult<AssignorView> search(String query, int page, int size);
}
