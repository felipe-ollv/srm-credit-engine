import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiFailure, Currency } from '../../core/http/api.models';
import { toApiFailure } from '../../core/http/api-error';
import { ReceivableResponse } from '../receivables/receivables.models';
import { IdempotencyAttempt } from './idempotency-attempt';
import { SettlementsApi } from './settlements.api';
import { SettlementBatchRequest, SettlementBatchResponse, SettlementSelection } from './settlements.models';

@Injectable()
export class SettlementsFacade {
  private readonly api = inject(SettlementsApi);
  private readonly attempt = new IdempotencyAttempt();
  private lastRequest: SettlementBatchRequest | null = null;
  readonly receivables = signal<readonly ReceivableResponse[]>([]);
  readonly selections = signal<ReadonlyMap<string, Currency>>(new Map());
  readonly loading = signal(false);
  readonly result = signal<SettlementBatchResponse | null>(null);
  readonly failure = signal<ApiFailure | null>(null);
  readonly selected = computed<readonly SettlementSelection[]>(() => this.receivables()
    .filter((receivable) => this.selections().has(receivable.id))
    .map((receivable) => ({ receivable, paymentCurrency: this.selections().get(receivable.id) ?? 'BRL' })));

  load(): void {
    this.api.availableReceivables().subscribe({
      next: (page) => this.receivables.set(page.content),
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Não foi possível carregar os recebíveis disponíveis.')),
    });
  }

  toggle(receivableId: string, checked: boolean): void {
    const next = new Map(this.selections());
    if (checked) next.set(receivableId, 'BRL'); else next.delete(receivableId);
    this.selections.set(next);
    this.invalidateResult();
  }

  setCurrency(receivableId: string, currency: Currency): void {
    const next = new Map(this.selections());
    if (next.has(receivableId)) next.set(receivableId, currency);
    this.selections.set(next);
    this.invalidateResult();
  }

  submit(): void {
    const request = this.toRequest();
    if (request.items.length === 0) return;
    this.lastRequest = request;
    this.execute(request);
  }

  retry(): void {
    if (this.lastRequest) this.execute(this.lastRequest);
  }

  private execute(request: SettlementBatchRequest): void {
    this.loading.set(true);
    this.failure.set(null);
    const key = this.attempt.keyFor(request);
    this.api.settle(request, key).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (result) => {
        this.result.set(result);
        this.attempt.complete();
        this.lastRequest = null;
        this.selections.set(new Map());
        this.load();
      },
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Não foi possível concluir a liquidação. Tente novamente com segurança.')),
    });
  }

  private toRequest(): SettlementBatchRequest {
    return { items: this.selected().map((item) => ({ receivableId: item.receivable.id, paymentCurrency: item.paymentCurrency })) };
  }

  private invalidateResult(): void {
    this.attempt.complete();
    this.result.set(null);
    this.failure.set(null);
    this.lastRequest = null;
  }
}
