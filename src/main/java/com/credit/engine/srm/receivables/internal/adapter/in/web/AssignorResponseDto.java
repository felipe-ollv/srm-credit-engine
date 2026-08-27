package com.credit.engine.srm.receivables.internal.adapter.in.web;

import com.credit.engine.srm.receivables.AssignorView;

import java.time.Instant;
import java.util.UUID;

record AssignorResponseDto(UUID id, String document, String legalName, Instant createdAt) {

    static AssignorResponseDto from(AssignorView view) {
        return new AssignorResponseDto(
                view.id().value(), view.document(), view.legalName(), view.createdAt());
    }
}
