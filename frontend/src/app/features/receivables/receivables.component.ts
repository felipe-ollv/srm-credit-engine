import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { pricingDateBounds, dueDateBoundsValidator } from '../pricing/date.util';
import { brlMoneyValidator, formatBrlMoney, normalizeBrlMoney } from '../pricing/money.util';
import { MoneyDisplayPipe } from '../pricing/pricing-display.pipe';
import { ReceivablesApi } from './receivables.api';
import { ReceivablesFacade } from './receivables.facade';
import { ReceivableStatus } from './receivables.models';
import { ReceivableType } from '../../core/http/api.models';

@Component({
  selector: 'app-receivables',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MoneyDisplayPipe],
  providers: [ReceivablesApi, ReceivablesFacade],
  template: `
    <main class="operations-page" aria-labelledby="receivables-title">
      <header class="operations-header"><div><h1 id="receivables-title">Recebíveis</h1><p>Registre títulos em BRL vinculados a um cedente.</p></div></header>
      <div class="operations-grid">
        <section class="operations-panel"><h2>Novo recebível</h2>
          <form class="operations-form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <mat-form-field appearance="outline"><mat-label>Cedente</mat-label><mat-select formControlName="assignorId">
              @for (assignor of facade.assignors(); track assignor.id) { <mat-option [value]="assignor.id">{{ assignor.legalName }} — {{ assignor.document }}</mat-option> }
            </mat-select><mat-error>Selecione o cedente.</mat-error></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Tipo</mat-label><mat-select formControlName="type"><mat-option value="DUPLICATA_MERCANTIL">Duplicata Mercantil</mat-option><mat-option value="CHEQUE_PRE_DATADO">Cheque Pré-datado</mat-option></mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Valor de face</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput formControlName="faceValue" inputmode="decimal" (blur)="formatFace()">
              @if (faceValue.touched && faceValue.invalid) { <mat-error>Informe um valor positivo com até duas casas.</mat-error> }
            </mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Vencimento</mat-label><input matInput type="date" formControlName="dueDate" [min]="bounds.minimum" [max]="bounds.maximum"><mat-error>Informe uma data futura em até 360 meses.</mat-error></mat-form-field>
            <button mat-flat-button type="submit" [disabled]="form.invalid || facade.busy()">Cadastrar recebível</button>
          </form>
          @if (facade.saved()) { <p class="feedback" role="status">Recebível cadastrado com sucesso.</p> }
          @if (facade.failure(); as failure) { <p class="feedback error" role="alert">{{ failure.message }} @if (failure.correlationId) { <code>{{ failure.correlationId }}</code> }</p> }
        </section>
        <section class="operations-panel"><h2>Consulta</h2>
          <form class="actions" [formGroup]="filters" (ngSubmit)="search()">
            <mat-form-field appearance="outline"><mat-label>Cedente</mat-label><mat-select formControlName="assignorId"><mat-option value="">Todos</mat-option>@for (assignor of facade.assignors(); track assignor.id) { <mat-option [value]="assignor.id">{{ assignor.legalName }}</mat-option> }</mat-select></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Status</mat-label><mat-select formControlName="status"><mat-option value="">Todos</mat-option><mat-option value="AVAILABLE">Disponível</mat-option><mat-option value="SETTLED">Liquidado</mat-option></mat-select></mat-form-field>
            <button mat-stroked-button type="submit">Filtrar</button>
          </form>
          <div class="table-scroll"><table class="data-table"><thead><tr><th>Tipo</th><th>Face</th><th>Vencimento</th><th>Status</th></tr></thead><tbody>
            @for (item of facade.page()?.content ?? []; track item.id) { <tr><td>{{ typeLabel(item.type) }}</td><td>{{ item.faceValue | moneyDisplay }}</td><td>{{ item.dueDate }}</td><td><span class="status">{{ item.status }}</span></td></tr> }
            @empty { <tr><td class="empty-row" colspan="4">Nenhum recebível encontrado.</td></tr> }
          </tbody></table></div>
          @if (facade.page(); as page) { <div class="pager"><button mat-button [disabled]="page.page === 0" (click)="go(page.page - 1)">Anterior</button><span>Página {{ page.page + 1 }} de {{ page.totalPages || 1 }}</span><button mat-button [disabled]="page.page + 1 >= page.totalPages" (click)="go(page.page + 1)">Próxima</button></div> }
        </section>
      </div>
    </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReceivablesComponent {
  private readonly builder = inject(FormBuilder);
  readonly facade = inject(ReceivablesFacade);
  readonly bounds = pricingDateBounds();
  readonly form = this.builder.nonNullable.group({
    assignorId: ['', Validators.required],
    type: this.builder.nonNullable.control<ReceivableType>('DUPLICATA_MERCANTIL'),
    faceValue: ['', [Validators.required, brlMoneyValidator()]],
    dueDate: ['', [Validators.required, dueDateBoundsValidator(this.bounds)]],
  });
  readonly filters = this.builder.nonNullable.group({
    assignorId: [''],
    status: this.builder.nonNullable.control<ReceivableStatus | ''>(''),
  });
  readonly faceValue = this.form.controls.faceValue;

  constructor() { this.facade.initialize(); }
  formatFace(): void { const value = formatBrlMoney(this.faceValue.value); if (value) this.faceValue.setValue(value); }
  search(): void { const value = this.filters.getRawValue(); this.facade.load(value.assignorId, value.status, 0); }
  go(page: number): void { const value = this.filters.getRawValue(); this.facade.load(value.assignorId, value.status, page); }
  typeLabel(type: ReceivableType): string { return type === 'DUPLICATA_MERCANTIL' ? 'Duplicata' : 'Cheque pré-datado'; }
  submit(): void {
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    const faceValue = normalizeBrlMoney(value.faceValue);
    if (this.form.invalid || faceValue === null) return;
    this.facade.create({ ...value, faceValue }, () => this.form.reset({ assignorId: '', type: 'DUPLICATA_MERCANTIL', faceValue: '', dueDate: '' }));
  }
}
