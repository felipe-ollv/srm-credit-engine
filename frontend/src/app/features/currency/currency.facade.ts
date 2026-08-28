import { inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiFailure, ExchangeRateResponse } from '../../core/http/api.models';
import { toApiFailure } from '../../core/http/api-error';
import { CurrencyApi } from './currency.api';

@Injectable()
export class CurrencyFacade {
  private readonly api = inject(CurrencyApi);
  readonly rate = signal<ExchangeRateResponse | null>(null);
  readonly loading = signal(false);
  readonly failure = signal<ApiFailure | null>(null);

  load(): void {
    this.api.current().subscribe({
      next: (rate) => this.rate.set(rate),
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Ainda não há uma cotação USD/BRL vigente.')),
    });
  }

  refresh(): void {
    this.loading.set(true);
    this.failure.set(null);
    this.api.refresh().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (rate) => this.rate.set(rate),
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'O provedor cambial está temporariamente indisponível.')),
    });
  }
}
