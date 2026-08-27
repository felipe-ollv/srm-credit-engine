package com.credit.engine.srm.receivables.internal.adapter.in.web;

import com.credit.engine.srm.shared.PageResult;

import java.util.List;
import java.util.function.Function;

record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static <S, T> PageResponseDto<T> from(PageResult<S> result, Function<S, T> mapper) {
        return new PageResponseDto<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
