package com.credit.engine.srm.receivables;

public interface CreateReceivableUseCase {

    ReceivableView create(CreateReceivableCommand command);
}
