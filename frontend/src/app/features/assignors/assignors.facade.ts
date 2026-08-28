import { inject, Injectable, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiFailure } from '../../core/http/api.models';
import { toApiFailure } from '../../core/http/api-error';
import { AssignorsApi } from './assignors.api';
import { AssignorPage, CreateAssignorRequest } from './assignors.models';

@Injectable()
export class AssignorsFacade {
  private readonly api = inject(AssignorsApi);
  private query = '';
  readonly page = signal<AssignorPage | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly failure = signal<ApiFailure | null>(null);
  readonly saved = signal(false);

  load(query = this.query, page = 0): void {
    this.query = query;
    this.loading.set(true);
    this.failure.set(null);
    this.api.search(query, page).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (result) => this.page.set(result),
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Não foi possível consultar os cedentes.')),
    });
  }

  create(request: CreateAssignorRequest, completed: () => void): void {
    this.saving.set(true);
    this.saved.set(false);
    this.failure.set(null);
    this.api.create(request).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.saved.set(true);
        completed();
        this.load('', 0);
      },
      error: (error: unknown) => this.failure.set(toApiFailure(error, 'Não foi possível cadastrar o cedente.')),
    });
  }
}
