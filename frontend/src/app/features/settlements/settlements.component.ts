import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Currency } from '../../core/http/api.models';
import { MoneyDisplayPipe } from '../pricing/pricing-display.pipe';
import { SettlementsApi } from './settlements.api';
import { SettlementsFacade } from './settlements.facade';
import { SettlementBatchItemResponse } from './settlements.models';

@Component({
  selector: 'app-settlements',
  imports: [MatButtonModule, MatCheckboxModule, MatProgressBarModule, MatSelectModule, MoneyDisplayPipe],
  providers: [SettlementsApi, SettlementsFacade],
  template: `
    <main class="operations-page" aria-labelledby="settlements-title">
      <header class="operations-header"><div><h1 id="settlements-title">Liquidação em lote</h1><p>Selecione de 1 a 100 recebíveis. Cada item possui transação independente.</p></div><button mat-flat-button [disabled]="facade.selected().length === 0 || facade.loading()" (click)="facade.submit()">Liquidar {{ facade.selected().length }} item(ns)</button></header>
      @if (facade.loading()) { <mat-progress-bar mode="indeterminate" aria-label="Liquidação em processamento" /> }
      @if (facade.failure(); as failure) { <div class="feedback error" role="alert"><strong>{{ failure.message }}</strong> @if (failure.correlationId) { <code>{{ failure.correlationId }}</code> } <button mat-stroked-button (click)="facade.retry()">Repetir a mesma tentativa</button></div> }
      <section class="operations-panel"><h2>Recebíveis disponíveis</h2><div class="table-scroll"><table class="data-table"><thead><tr><th>Selecionar</th><th>Recebível</th><th>Face</th><th>Vencimento</th><th>Pagamento</th></tr></thead><tbody>
        @for (receivable of facade.receivables(); track receivable.id) { <tr>
          <td><mat-checkbox [checked]="facade.selections().has(receivable.id)" [attr.aria-label]="'Selecionar recebível ' + receivable.id" (change)="facade.toggle(receivable.id, $event.checked)" /></td>
          <td><code>{{ shortId(receivable.id) }}</code><br><small>{{ receivable.type }}</small></td><td>{{ receivable.faceValue | moneyDisplay }}</td><td>{{ receivable.dueDate }}</td>
          <td><mat-select aria-label="Moeda de pagamento" [disabled]="!facade.selections().has(receivable.id)" [value]="facade.selections().get(receivable.id) ?? 'BRL'" (selectionChange)="facade.setCurrency(receivable.id, $event.value)"><mat-option value="BRL">BRL</mat-option><mat-option value="USD">USD</mat-option></mat-select></td>
        </tr> } @empty { <tr><td colspan="5" class="empty-row">Não há recebíveis disponíveis.</td></tr> }
      </tbody></table></div></section>
      @if (facade.result(); as result) { <section class="operations-panel results" aria-live="polite"><h2>Resultado do lote <code>{{ shortId(result.batchId) }}</code></h2><div class="table-scroll"><table class="data-table"><thead><tr><th>Recebível</th><th>Status</th><th>Pagamento</th><th>Detalhe</th></tr></thead><tbody>
        @for (item of result.items; track item.receivableId) { <tr><td><code>{{ shortId(item.receivableId) }}</code></td><td><span class="status" [class.conflict]="item.status !== 'SUCCESS'">{{ statusLabel(item) }}</span></td><td>{{ item.settlement?.payment ? (item.settlement!.payment | moneyDisplay) : '—' }}</td><td>{{ item.detail ?? 'Liquidado com sucesso' }}</td></tr> }
      </tbody></table></div></section> }
    </main>
  `,
  styles: `.results { margin-top: 1.25rem; } mat-select { min-width: 5rem; } code { font-size: .75rem; }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettlementsComponent {
  readonly facade = inject(SettlementsFacade);
  constructor() { this.facade.load(); }
  shortId(id: string): string { return id.slice(0, 8); }
  statusLabel(item: SettlementBatchItemResponse): string {
    const labels: Record<string, string> = { SUCCESS: 'Sucesso', CONFLICT: 'Conflito', FX_RATE_UNAVAILABLE: 'Câmbio indisponível', NOT_FOUND: 'Não encontrado', RULE_VIOLATION: 'Regra violada', TECHNICAL_ERROR: 'Erro técnico' };
    return labels[item.status] ?? item.status;
  }
  currency(value: string): Currency { return value as Currency; }
}
