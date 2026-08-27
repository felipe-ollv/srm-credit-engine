package com.credit.engine.srm.receivables;

import com.credit.engine.srm.shared.PageResult;

public interface SearchReceivablesUseCase {

    PageResult<ReceivableView> search(SearchReceivablesQuery query);
}
