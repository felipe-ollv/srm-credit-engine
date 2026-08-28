import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Currency } from '../../core/http/api.models';
import { DateTimePtPipe, MoneyDisplayPipe } from '../pricing/pricing-display.pipe';
import { ReportingApi } from './reporting.api';
import { ReportingFacade } from './reporting.facade';
import { ReportingFilters } from './reporting.models';

@Component({
  selector: 'app-reporting',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatProgressBarModule, MatSelectModule, DateTimePtPipe, MoneyDisplayPipe],
  providers: [ReportingApi, ReportingFacade],
  template: `
    <main class="operations-page" aria-labelledby="report-title">
      <header class="operations-header"><div><h1 id="report-title">Extrato de liquidações</h1><p>Consulta analítica baseada nos snapshots financeiros imutáveis.</p></div></header>
      <section class="operations-panel">
        <form class="filters" [formGroup]="form" (ngSubmit)="search()">
          <mat-form-field appearance="outline"><mat-label>De</mat-label><input matInput type="date" formControlName="from"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Até</mat-label><input matInput type="date" formControlName="to"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Cedente</mat-label><mat-select formControlName="assignorId"><mat-option value="">Todos</mat-option>@for (assignor of facade.assignors(); track assignor.id) { <mat-option [value]="assignor.id">{{ assignor.legalName }}</mat-option> }</mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Moeda</mat-label><mat-select formControlName="paymentCurrency"><mat-option value="">Todas</mat-option><mat-option value="BRL">BRL</mat-option><mat-option value="USD">USD</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Ordenação</mat-label><mat-select formControlName="sort"><mat-option value="settledAt,desc">Mais recentes</mat-option><mat-option value="settledAt,asc">Mais antigas</mat-option><mat-option value="assignorLegalName,asc">Cedente</mat-option><mat-option value="paymentAmount,desc">Maior pagamento</mat-option></mat-select></mat-form-field>
          <button mat-flat-button type="submit">Aplicar filtros</button>
        </form>
        @if (facade.loading()) { <mat-progress-bar mode="indeterminate" aria-label="Carregando extrato" /> }
        @if (facade.failure(); as failure) { <p class="feedback error" role="alert">{{ failure.message }} @if (failure.correlationId) { <code>{{ failure.correlationId }}</code> }</p> }
        <div class="table-scroll"><table class="data-table"><thead><tr><th>Liquidação</th><th>Cedente</th><th>Face</th><th>Deságio</th><th>Pagamento</th><th>Data</th></tr></thead><tbody>
          @for (item of facade.page()?.content ?? []; track item.settlementId) { <tr><td><code>{{ shortId(item.settlementId) }}</code><br><small>{{ item.receivableType }}</small></td><td>{{ item.assignorLegalName }}<br><small>{{ item.assignorDocument }}</small></td><td>{{ item.faceValue | moneyDisplay }}</td><td>{{ item.discount | moneyDisplay }}</td><td><strong>{{ item.payment | moneyDisplay }}</strong></td><td>{{ item.settledAt | dateTimePt }}</td></tr> }
          @empty { <tr><td colspan="6" class="empty-row">Nenhuma liquidação encontrada.</td></tr> }
        </tbody></table></div>
        @if (facade.page(); as page) { <div class="pager"><button mat-button [disabled]="page.page === 0" (click)="go(page.page - 1)">Anterior</button><span>{{ page.totalElements }} registro(s) — página {{ page.page + 1 }} de {{ page.totalPages || 1 }}</span><button mat-button [disabled]="page.page + 1 >= page.totalPages" (click)="go(page.page + 1)">Próxima</button></div> }
      </section>
    </main>
  `,
  styles: `.filters { display: grid; grid-template-columns: repeat(5, minmax(8rem, 1fr)) auto; gap: .6rem; align-items: start; } @media (max-width: 950px) { .filters { grid-template-columns: repeat(2, 1fr); } } @media (max-width: 560px) { .filters { grid-template-columns: 1fr; } }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportingComponent {
  private readonly builder = inject(FormBuilder);
  readonly facade = inject(ReportingFacade);
  readonly form = this.builder.nonNullable.group({
    from: [''], to: [''], assignorId: [''],
    paymentCurrency: this.builder.nonNullable.control<Currency | ''>(''),
    sort: this.builder.nonNullable.control<ReportingFilters['sort']>('settledAt,desc'),
  });
  constructor() { this.facade.initialize(); }
  search(): void { this.facade.load(this.form.getRawValue(), 0); }
  go(page: number): void { this.facade.load(this.form.getRawValue(), page); }
  shortId(id: string): string { return id.slice(0, 8); }
}
