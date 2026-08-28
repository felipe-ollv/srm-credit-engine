import { inject, Injectable, signal } from '@angular/core';
import { ApiFailure } from '../../core/http/api.models';
import { toApiFailure } from '../../core/http/api-error';
import { AssignorOption } from '../receivables/receivables.models';
import { ReportingApi } from './reporting.api';
import { ReportingFilters, SettlementPage } from './reporting.models';

const DEFAULT_FILTERS: ReportingFilters = {
  from: '', to: '', assignorId: '', paymentCurrency: '', sort: 'settledAt,desc',
};

@Injectable()
export class ReportingFacade {
  private readonly api = inject(ReportingApi);
  private filters: ReportingFilters = DEFAULT_FILTERS;
  readonly assignors = signal<readonly AssignorOption[]>([]);
  readonly page = signal<SettlementPage | null>(null);
  readonly failure = signal<ApiFailure | null>(null);
  readonly loading = signal(false);

  initialize(): void {
    this.api.assignors().subscribe({ next: (page) => this.assignors.set(page.content) });
    this.load(DEFAULT_FILTERS, 0);
  }

  load(filters = this.filters, page = 0): void {
    this.filters = filters;
    this.loading.set(true);
    this.failure.set(null);
    this.api.search(filters, page).subscribe({
      next: (result) => { this.page.set(result); this.loading.set(false); },
      error: (error: unknown) => { this.failure.set(toApiFailure(error, 'Não foi possível consultar o extrato.')); this.loading.set(false); },
    });
  }
}
