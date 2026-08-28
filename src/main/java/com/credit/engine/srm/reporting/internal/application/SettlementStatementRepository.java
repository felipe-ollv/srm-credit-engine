package com.credit.engine.srm.reporting.internal.application;

import com.credit.engine.srm.reporting.SettlementStatement;
import com.credit.engine.srm.shared.PageResult;

public interface SettlementStatementRepository {

    PageResult<SettlementStatement> search(SettlementSearchCriteria criteria);
}
