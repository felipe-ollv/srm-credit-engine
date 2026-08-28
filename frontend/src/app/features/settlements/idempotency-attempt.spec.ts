import { describe, expect, it } from 'vitest';
import { IdempotencyAttempt } from './idempotency-attempt';
import { SettlementBatchRequest } from './settlements.models';

describe('IdempotencyAttempt', () => {
  const first: SettlementBatchRequest = { items: [{ receivableId: 'a', paymentCurrency: 'BRL' }] };
  const changed: SettlementBatchRequest = { items: [{ receivableId: 'a', paymentCurrency: 'USD' }] };

  it('reuses the key for an unchanged retry', () => {
    const attempt = new IdempotencyAttempt();
    expect(attempt.keyFor(first)).toBe(attempt.keyFor(first));
  });

  it('creates a key when the payload changes', () => {
    const attempt = new IdempotencyAttempt();
    expect(attempt.keyFor(first)).not.toBe(attempt.keyFor(changed));
  });

  it('creates a new key after a completed response', () => {
    const attempt = new IdempotencyAttempt();
    const oldKey = attempt.keyFor(first);
    attempt.complete();
    expect(attempt.keyFor(first)).not.toBe(oldKey);
  });
});
