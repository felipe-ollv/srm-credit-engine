package com.credit.engine.srm.reporting.internal.adapter.in.web;

import com.credit.engine.srm.reporting.SettlementStatement;
import com.credit.engine.srm.shared.PageResult;

import java.util.List;

record SettlementPageResponseDto(
        List<SettlementStatementResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static SettlementPageResponseDto from(PageResult<SettlementStatement> result) {
        return new SettlementPageResponseDto(
                result.content().stream().map(SettlementStatementResponseDto::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
