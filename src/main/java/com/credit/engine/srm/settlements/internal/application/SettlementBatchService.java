package com.credit.engine.srm.settlements.internal.application;

import com.credit.engine.srm.currency.FindCurrentExchangeRateUseCase;
import com.credit.engine.srm.settlements.CreateSettlementBatchUseCase;
import com.credit.engine.srm.settlements.IdempotencyConflictException;
import com.credit.engine.srm.settlements.InvalidSettlementBatchException;
import com.credit.engine.srm.settlements.SettlementBatchCommand;
import com.credit.engine.srm.settlements.SettlementBatchItemResult;
import com.credit.engine.srm.settlements.SettlementBatchResult;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SettlementBatchService implements CreateSettlementBatchUseCase {

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    private final IdempotencyCoordinator idempotency;
    private final IdempotencyResponseCodec codec;
    private final SettlementItemProcessor items;
    private final FindCurrentExchangeRateUseCase exchangeRates;
    private final Clock clock;

    SettlementBatchService(
            IdempotencyCoordinator idempotency,
            IdempotencyResponseCodec codec,
            SettlementItemProcessor items,
            FindCurrentExchangeRateUseCase exchangeRates,
            Clock clock) {
        this.idempotency = idempotency;
        this.codec = codec;
        this.items = items;
        this.exchangeRates = exchangeRates;
        this.clock = clock;
    }

    @Override
    public SettlementBatchResult create(String idempotencyKey, SettlementBatchCommand command) {
        validate(idempotencyKey, command);
        String requestHash = hash(command);
        Optional<IdempotencyRecord> existing = idempotency.find(idempotencyKey);
        if (existing.isPresent()) {
            return resolve(existing.orElseThrow(), requestHash);
        }

        Instant requestedAt = clock.instant();
        UUID batchId = UUID.randomUUID();
        try {
            idempotency.claim(idempotencyKey, requestHash, batchId, requestedAt);
        } catch (DataIntegrityViolationException exception) {
            IdempotencyRecord raced = idempotency.find(idempotencyKey)
                    .orElseThrow(() -> new IdempotencyConflictException(
                            "IDEMPOTENCY_IN_PROGRESS",
                            "Idempotency key is being claimed by another request"));
            return resolve(raced, requestHash);
        }

        Optional<ExchangeRate> batchRate = requiresUsd(command)
                ? exchangeRates.find(Currency.USD, Currency.BRL, requestedAt)
                : Optional.empty();
        List<SettlementBatchItemResult> results = new ArrayList<>(command.items().size());
        for (int index = 0; index < command.items().size(); index++) {
            results.add(items.process(
                    batchId, index, command.items().get(index), requestedAt, batchRate));
        }
        Instant completedAt = clock.instant();
        SettlementBatchResult result = new SettlementBatchResult(
                batchId, "COMPLETED", requestedAt, completedAt, List.copyOf(results));
        idempotency.complete(idempotencyKey, batchId, codec.encode(result), completedAt);
        return result;
    }

    private SettlementBatchResult resolve(IdempotencyRecord record, String requestHash) {
        if (!record.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "IDEMPOTENCY_PAYLOAD_CONFLICT",
                    "Idempotency key was already used with a different payload");
        }
        if (!"COMPLETED".equals(record.status())) {
            throw new IdempotencyConflictException(
                    "IDEMPOTENCY_IN_PROGRESS",
                    "A request with this idempotency key is still processing");
        }
        return codec.decode(record.responsePayload());
    }

    private static void validate(String key, SettlementBatchCommand command) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new InvalidSettlementBatchException(
                    "Idempotency-Key must contain 1 to 64 safe ASCII characters");
        }
        if (command == null || command.items().isEmpty() || command.items().size() > 100) {
            throw new InvalidSettlementBatchException("Settlement batch must contain between 1 and 100 items");
        }
        HashSet<UUID> receivableIds = new HashSet<>();
        for (SettlementBatchCommand.Item item : command.items()) {
            if (!receivableIds.add(item.receivableId().value())) {
                throw new InvalidSettlementBatchException(
                        "A receivableId cannot be repeated within the same batch");
            }
        }
    }

    private static boolean requiresUsd(SettlementBatchCommand command) {
        return command.items().stream()
                .anyMatch(item -> item.paymentCurrency() == Currency.USD);
    }

    private static String hash(SettlementBatchCommand command) {
        StringBuilder normalized = new StringBuilder();
        command.items().forEach(item -> normalized
                .append(item.receivableId().value())
                .append(':')
                .append(item.paymentCurrency().name())
                .append('\n'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
