package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.PageResult;

public interface SearchAssignorsUseCase {

    PageResult<AssignorView> search(SearchAssignorsQuery query);
}
