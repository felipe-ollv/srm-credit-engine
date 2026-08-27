package com.credit.engine.srm.receivables.internal.adapter.in.web;

import com.credit.engine.srm.receivables.ReceivableView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record ReceivableResponseDto(
        UUID id,
        UUID assignorId,
        String type,
        MoneyResponseDto faceValue,
        LocalDate dueDate,
        LocalDate registrationDate,
        String status,
        Instant createdAt) {

    static ReceivableResponseDto from(ReceivableView view) {
        return new ReceivableResponseDto(
                view.id().value(),
                view.assignorId().value(),
                view.type().name(),
                MoneyResponseDto.from(view.faceValue()),
                view.dueDate(),
                view.registrationDate(),
                view.status().name(),
                view.createdAt());
    }
}
