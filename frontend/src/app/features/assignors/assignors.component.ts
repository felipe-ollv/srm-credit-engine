import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AssignorsApi } from './assignors.api';
import { AssignorsFacade } from './assignors.facade';
import { cnpjValidator, formatCnpj, normalizeCnpj } from './cnpj.util';

@Component({
  selector: 'app-assignors',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  providers: [AssignorsApi, AssignorsFacade],
  template: `
    <main class="operations-page" aria-labelledby="assignors-title">
      <header class="operations-header"><div><h1 id="assignors-title">Cedentes</h1><p>Cadastre e consulte empresas cedentes.</p></div></header>
      <div class="operations-grid">
        <section class="operations-panel"><h2>Novo cedente</h2>
          <form class="operations-form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <mat-form-field appearance="outline"><mat-label>CNPJ</mat-label>
              <input matInput formControlName="document" inputmode="numeric" autocomplete="off" (blur)="formatDocument()">
              @if (document.touched && document.hasError('required')) { <mat-error>Informe o CNPJ.</mat-error> }
              @else if (document.touched && document.hasError('invalidCnpj')) { <mat-error>Informe um CNPJ válido.</mat-error> }
            </mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Razão social</mat-label>
              <input matInput formControlName="legalName" maxlength="160" autocomplete="organization">
              @if (legalName.touched && legalName.invalid) { <mat-error>Use entre 2 e 160 caracteres.</mat-error> }
            </mat-form-field>
            <div class="actions"><button mat-flat-button type="submit" [disabled]="form.invalid || facade.saving()">Cadastrar</button></div>
          </form>
          @if (facade.saved()) { <p class="feedback" role="status">Cedente cadastrado com sucesso.</p> }
          @if (facade.failure(); as failure) { <p class="feedback error" role="alert">{{ failure.message }} @if (failure.correlationId) { <code>{{ failure.correlationId }}</code> }</p> }
        </section>
        <section class="operations-panel"><h2>Consulta</h2>
          <form class="actions" [formGroup]="searchForm" (ngSubmit)="search()">
            <mat-form-field appearance="outline"><mat-label>CNPJ ou razão social</mat-label><input matInput formControlName="query"></mat-form-field>
            <button mat-stroked-button type="submit">Buscar</button>
          </form>
          <div class="table-scroll"><table class="data-table"><thead><tr><th>CNPJ</th><th>Razão social</th><th>Cadastro</th></tr></thead><tbody>
            @for (assignor of facade.page()?.content ?? []; track assignor.id) { <tr><td>{{ assignor.document }}</td><td>{{ assignor.legalName }}</td><td>{{ assignor.createdAt.slice(0, 10) }}</td></tr> }
            @empty { <tr><td class="empty-row" colspan="3">Nenhum cedente encontrado.</td></tr> }
          </tbody></table></div>
          @if (facade.page(); as page) { <div class="pager"><button mat-button type="button" [disabled]="page.page === 0" (click)="go(page.page - 1)">Anterior</button><span>Página {{ page.page + 1 }} de {{ page.totalPages || 1 }}</span><button mat-button type="button" [disabled]="page.page + 1 >= page.totalPages" (click)="go(page.page + 1)">Próxima</button></div> }
        </section>
      </div>
    </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignorsComponent {
  private readonly builder = inject(FormBuilder);
  readonly facade = inject(AssignorsFacade);
  readonly form = this.builder.nonNullable.group({
    document: ['', [Validators.required, cnpjValidator()]],
    legalName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(160)]],
  });
  readonly searchForm = this.builder.nonNullable.group({ query: [''] });
  readonly document = this.form.controls.document;
  readonly legalName = this.form.controls.legalName;

  constructor() { this.facade.load(); }

  formatDocument(): void { this.document.setValue(formatCnpj(this.document.value)); }
  search(): void { this.facade.load(this.searchForm.controls.query.value, 0); }
  go(page: number): void { this.facade.load(this.searchForm.controls.query.value, page); }
  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.facade.create({ document: normalizeCnpj(value.document), legalName: value.legalName.trim() }, () => this.form.reset());
  }
}
