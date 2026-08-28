package com.credit.engine.srm.reporting;

import com.credit.engine.srm.shared.PageResult;

public interface SearchSettlementsUseCase {

    PageResult<SettlementStatement> search(SettlementSearchQuery query);
}
