import { inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiFailure } from '../../core/http/api.models';
import { toApiFailure } from '../../core/http/api-error';
import { ReceivablesApi } from './receivables.api';
import {
  AssignorOption,
  CreateReceivableRequest,
  ReceivablePage,
  ReceivableStatus,
} from './receivables.models';

@Injectable()
export class ReceivablesFacade {
  private readonly api = inject(ReceivablesApi);
  private assignorFilter = '';
  private statusFilter: ReceivableStatus | '' = '';
  readonly assignors = signal<readonly AssignorOption[]>([]);
  readonly page = signal<ReceivablePage | null>(null);
  readonly busy = signal(false);
  readonly failure = signal<ApiFailure | null>(null);
  readonly saved = signal(false);

  initialize(): void {
    this.api.assignors().subscribe({
      next: (result) => this.assignors.set(result.content),
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Não foi possível carregar os cedentes.')),
    });
    this.load();
  }

  load(assignorId = this.assignorFilter, status = this.statusFilter, page = 0): void {
    this.assignorFilter = assignorId;
    this.statusFilter = status;
    this.api.search(assignorId, status, page).subscribe({
      next: (result) => this.page.set(result),
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Não foi possível consultar os recebíveis.')),
    });
  }

  create(request: CreateReceivableRequest, completed: () => void): void {
    this.busy.set(true);
    this.saved.set(false);
    this.failure.set(null);
    this.api.create(request).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        this.saved.set(true);
        completed();
        this.load('', '', 0);
      },
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Não foi possível cadastrar o recebível.')),
    });
  }
}
