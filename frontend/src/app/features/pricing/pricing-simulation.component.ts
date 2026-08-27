import {
  ChangeDetectionStrategy,
  Component,
  effect,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { startWith, map, Observable, tap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { PricingApi } from './pricing.api';
import { PricingFacade } from './pricing.facade';
import {
  Currency,
  PricingSimulationRequest,
  ReceivableType,
} from './pricing.models';
import {
  brlMoneyValidator,
  formatBrlMoney,
  normalizeBrlMoney,
} from './money.util';
import {
  DateBounds,
  dueDateBoundsValidator,
  pricingDateBounds,
} from './date.util';
import {
  DateTimePtPipe,
  DecimalDisplayPipe,
  MoneyDisplayPipe,
  RatePercentPipe,
} from './pricing-display.pipe';

@Component({
  selector: 'app-pricing-simulation',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    MoneyDisplayPipe,
    DecimalDisplayPipe,
    RatePercentPipe,
    DateTimePtPipe,
  ],
  providers: [PricingApi, PricingFacade],
  template: `
    <main class="page" aria-labelledby="page-title">
      <header class="page-intro">
        <div>
          <p class="eyebrow">Motor de precificação</p>
          <h1 id="page-title">Simular antecipação</h1>
          <p>Informe os dados do recebível. O resultado é atualizado automaticamente.</p>
        </div>
        <span class="live-badge"><i aria-hidden="true"></i> Cálculo em tempo real</span>
      </header>

      <div class="workspace">
        <section class="panel form-panel" aria-labelledby="form-title">
          <div class="panel-heading">
            <span>01</span>
            <div>
              <h2 id="form-title">Dados do recebível</h2>
              <p>O valor de face é sempre denominado em reais.</p>
            </div>
          </div>

          <form [formGroup]="form" novalidate>
            <mat-form-field appearance="outline">
              <mat-label>Tipo de recebível</mat-label>
              <mat-select formControlName="receivableType">
                <mat-option value="DUPLICATA_MERCANTIL">Duplicata Mercantil</mat-option>
                <mat-option value="CHEQUE_PRE_DATADO">Cheque Pré-datado</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Valor de face</mat-label>
              <span matTextPrefix>R$&nbsp;</span>
              <input
                matInput
                formControlName="faceValue"
                inputmode="decimal"
                autocomplete="off"
                placeholder="100.000,00"
                aria-describedby="face-value-hint"
                (blur)="formatFaceValue()"
              >
              <mat-hint id="face-value-hint">Até 17 dígitos e duas casas decimais</mat-hint>
              @if (faceValue.touched && faceValue.hasError('required')) {
                <mat-error>Informe o valor de face.</mat-error>
              } @else if (faceValue.touched && faceValue.hasError('invalidMoney')) {
                <mat-error>Use um valor positivo no formato 100.000,00.</mat-error>
              } @else if (faceValue.hasError('server')) {
                <mat-error>{{ faceValue.getError('server') }}</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Data de vencimento</mat-label>
              <input
                matInput
                type="date"
                formControlName="dueDate"
                [min]="dateBounds.minimum"
                [max]="dateBounds.maximum"
              >
              <mat-hint>Prazo máximo de 360 meses</mat-hint>
              @if (dueDate.touched && dueDate.hasError('required')) {
                <mat-error>Informe a data de vencimento.</mat-error>
              } @else if (dueDate.touched && dueDate.hasError('dueDatePast')) {
                <mat-error>O vencimento deve ser futuro.</mat-error>
              } @else if (dueDate.touched && dueDate.hasError('dueDateTooFar')) {
                <mat-error>O prazo não pode superar 360 meses.</mat-error>
              } @else if (dueDate.hasError('server')) {
                <mat-error>{{ dueDate.getError('server') }}</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Moeda de pagamento</mat-label>
              <mat-select formControlName="paymentCurrency">
                <mat-option value="BRL">BRL — Real brasileiro</mat-option>
                <mat-option value="USD">USD — Dólar americano</mat-option>
              </mat-select>
            </mat-form-field>
          </form>

          <aside class="security-note">
            <strong>Cálculo protegido</strong>
            <span>Taxa base, spread e câmbio são definidos exclusivamente pelo servidor.</span>
          </aside>
        </section>

        <section class="panel result-panel" aria-labelledby="result-title" aria-live="polite">
          <div class="panel-heading">
            <span>02</span>
            <div>
              <h2 id="result-title">Resultado da simulação</h2>
              <p>Valores líquidos calculados pelo motor financeiro.</p>
            </div>
          </div>

          @if (facade.loading()) {
            <div class="state loading-state" role="status">
              <mat-progress-bar mode="indeterminate" />
              <strong>Calculando valor presente…</strong>
              <span>Aplicando prazo, taxas e conversão cambial.</span>
            </div>
          } @else if (facade.failure(); as failure) {
            <div class="state error-state" role="alert">
              <span class="state-symbol" aria-hidden="true">!</span>
              <strong>Não foi possível simular</strong>
              <p>{{ failure.message }}</p>
              @if (failure.correlationId) {
                <small>Identificador: <code>{{ failure.correlationId }}</code></small>
              }
              <button mat-stroked-button type="button" (click)="facade.retry()">Tentar novamente</button>
            </div>
          } @else if (facade.result(); as result) {
            <div class="result-content">
              <div class="payment-highlight">
                <span>Valor líquido a receber</span>
                <strong>{{ result.payment | moneyDisplay }}</strong>
                <small>Pagamento em {{ result.payment.currency }}</small>
              </div>

              <dl class="financial-summary">
                <div>
                  <dt>Valor de face</dt>
                  <dd>{{ result.faceValue | moneyDisplay }}</dd>
                </div>
                <div>
                  <dt>Valor presente</dt>
                  <dd>{{ result.presentValue | moneyDisplay }}</dd>
                </div>
                <div>
                  <dt>Deságio</dt>
                  <dd class="discount">− {{ result.discount | moneyDisplay }}</dd>
                </div>
              </dl>

              <mat-divider />

              <dl class="parameters">
                <div><dt>Prazo</dt><dd>{{ result.termMonths }} meses</dd></div>
                <div><dt>Taxa base</dt><dd>{{ result.baseRate | ratePercent }} a.m.</dd></div>
                <div><dt>Spread</dt><dd>{{ result.spread | ratePercent }} a.m.</dd></div>
              </dl>

              @if (result.exchangeRate; as exchangeRate) {
                <div class="fx-card">
                  <span>Cotação aplicada</span>
                  <strong>1 {{ exchangeRate.baseCurrency }} = R$ {{ exchangeRate.rate | decimalDisplay }}</strong>
                  <small>Capturada em {{ exchangeRate.capturedAt | dateTimePt }}</small>
                </div>
              }

              <p class="calculated-at">Calculado em {{ result.calculatedAt | dateTimePt }}</p>
            </div>
          } @else {
            <div class="state empty-state">
              <span class="state-symbol" aria-hidden="true">↗</span>
              <strong>Preencha os dados para começar</strong>
              <p>A simulação será executada quando todos os campos estiverem válidos.</p>
            </div>
          }

          <footer class="simulation-notice">
            Esta simulação é informativa, não reserva taxas e será recalculada na liquidação.
          </footer>
        </section>
      </div>
    </main>
  `,
  styles: `
    :host { display: block; }
    .page { width: min(1180px, calc(100% - 2rem)); margin: 0 auto; padding: clamp(2rem, 5vw, 4.5rem) 0; }
    .page-intro { display: flex; justify-content: space-between; align-items: end; gap: 2rem; margin-bottom: 2rem; }
    .eyebrow { margin: 0 0 .45rem; color: #0f766e; font-weight: 800; font-size: .75rem; text-transform: uppercase; letter-spacing: .12em; }
    h1 { margin: 0; color: #102a43; font-size: clamp(2rem, 4vw, 3.25rem); line-height: 1.05; letter-spacing: -.04em; }
    .page-intro p:not(.eyebrow) { margin: .8rem 0 0; color: #62778b; }
    .live-badge { display: flex; align-items: center; gap: .55rem; padding: .65rem .9rem; border: 1px solid #b8dcd7; border-radius: 999px; background: #eaf8f5; color: #0b6159; font-size: .78rem; font-weight: 700; white-space: nowrap; }
    .live-badge i { width: .5rem; height: .5rem; border-radius: 50%; background: #0d9488; box-shadow: 0 0 0 .25rem rgba(13, 148, 136, .12); }
    .workspace { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.08fr); gap: 1.25rem; align-items: stretch; }
    .panel { display: flex; flex-direction: column; min-height: 38rem; padding: clamp(1.25rem, 3vw, 2rem); border: 1px solid #dce6ed; border-radius: 1.25rem; background: rgba(255, 255, 255, .96); box-shadow: 0 .8rem 2.4rem rgba(16, 42, 67, .07); }
    .panel-heading { display: flex; align-items: flex-start; gap: .9rem; margin-bottom: 1.75rem; }
    .panel-heading > span { display: grid; place-items: center; flex: 0 0 2rem; height: 2rem; border-radius: .55rem; background: #e7f1f6; color: #31546f; font-size: .72rem; font-weight: 800; }
    h2 { margin: .1rem 0 .25rem; color: #163b57; font-size: 1.15rem; }
    .panel-heading p { margin: 0; color: #73879a; font-size: .82rem; }
    form { display: grid; gap: .45rem; }
    mat-form-field { width: 100%; }
    .security-note { display: flex; gap: .8rem; margin-top: auto; padding: 1rem; border-left: 3px solid #0f766e; border-radius: .25rem .7rem .7rem .25rem; background: #eff8f7; }
    .security-note strong { color: #0b6159; white-space: nowrap; }
    .security-note span { color: #4d6b6a; font-size: .8rem; line-height: 1.45; }
    .result-panel { position: relative; overflow: hidden; }
    .state { display: grid; justify-items: center; align-content: center; flex: 1; padding: 2rem; text-align: center; color: #60758a; }
    .state-symbol { display: grid; place-items: center; width: 4rem; height: 4rem; margin-bottom: 1rem; border-radius: 1.2rem; background: #eaf2f7; color: #1d5876; font-size: 2rem; font-weight: 300; }
    .state strong { color: #183b56; font-size: 1.05rem; }
    .state p, .state span { max-width: 24rem; }
    .loading-state { gap: 1rem; }
    .loading-state mat-progress-bar { width: min(20rem, 100%); }
    .error-state .state-symbol { background: #fff0ed; color: #b42318; }
    .error-state small { margin: .3rem 0 1rem; color: #65798c; }
    code { word-break: break-all; }
    .result-content { display: grid; gap: 1.35rem; }
    .payment-highlight { padding: 1.5rem; border-radius: 1rem; background: linear-gradient(135deg, #102a43, #174f64); color: white; }
    .payment-highlight span, .payment-highlight small { display: block; color: #bcd4df; }
    .payment-highlight strong { display: block; margin: .45rem 0; font-size: clamp(2rem, 5vw, 3rem); letter-spacing: -.04em; }
    .financial-summary, .parameters { display: grid; margin: 0; }
    .financial-summary { grid-template-columns: repeat(3, 1fr); gap: .75rem; }
    .financial-summary div { padding: .9rem; border-radius: .75rem; background: #f4f8fa; }
    dt { color: #718598; font-size: .73rem; }
    dd { margin: .35rem 0 0; color: #183b56; font-weight: 750; }
    dd.discount { color: #b54708; }
    .parameters { grid-template-columns: repeat(3, 1fr); gap: 1rem; }
    .fx-card { display: grid; gap: .25rem; padding: 1rem 1.15rem; border: 1px solid #bce0db; border-radius: .8rem; background: #effaf8; }
    .fx-card span, .fx-card small { color: #557874; font-size: .75rem; }
    .fx-card strong { color: #0d625a; }
    .calculated-at { margin: 0; color: #778b9d; font-size: .75rem; text-align: right; }
    .simulation-notice { margin-top: auto; padding-top: 1.2rem; color: #75899a; font-size: .72rem; line-height: 1.45; text-align: center; }
    @media (max-width: 900px) {
      .workspace { grid-template-columns: 1fr; }
      .panel { min-height: auto; }
      .result-panel { min-height: 34rem; }
    }
    @media (max-width: 620px) {
      .page-intro { align-items: flex-start; flex-direction: column; }
      .financial-summary, .parameters { grid-template-columns: 1fr; }
      .security-note { flex-direction: column; gap: .3rem; }
      .payment-highlight strong { font-size: 2rem; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PricingSimulationComponent {
  private readonly formBuilder = inject(FormBuilder);
  readonly facade = inject(PricingFacade);
  readonly dateBounds: DateBounds = pricingDateBounds();

  readonly form = this.formBuilder.nonNullable.group({
    receivableType: this.formBuilder.nonNullable.control<ReceivableType>(
      'DUPLICATA_MERCANTIL',
      Validators.required,
    ),
    faceValue: this.formBuilder.nonNullable.control('', [
      Validators.required,
      brlMoneyValidator(),
    ]),
    dueDate: this.formBuilder.nonNullable.control('', [
      Validators.required,
      dueDateBoundsValidator(this.dateBounds),
    ]),
    paymentCurrency: this.formBuilder.nonNullable.control<Currency>(
      'BRL',
      Validators.required,
    ),
  });

  readonly faceValue = this.form.controls.faceValue;
  readonly dueDate = this.form.controls.dueDate;
  private readonly commands: Observable<PricingSimulationRequest | null> = this.form.valueChanges.pipe(
    startWith(this.form.getRawValue()),
    tap(() => this.clearServerErrors()),
    map(() => this.toRequest()),
  );

  constructor() {
    this.facade.connect(this.commands);
    effect(() => this.applyServerErrors(this.facade.failure()?.fieldErrors ?? {}));
  }

  formatFaceValue(): void {
    this.faceValue.markAsTouched();
    const formatted = formatBrlMoney(this.faceValue.value);
    if (formatted !== null && formatted !== this.faceValue.value) {
      this.faceValue.setValue(formatted);
    }
  }

  private toRequest(): PricingSimulationRequest | null {
    if (this.form.invalid) {
      return null;
    }
    const value = this.form.getRawValue();
    const normalizedFaceValue = normalizeBrlMoney(value.faceValue);
    if (normalizedFaceValue === null) {
      return null;
    }
    return {
      receivableType: value.receivableType,
      faceValue: normalizedFaceValue,
      dueDate: value.dueDate,
      paymentCurrency: value.paymentCurrency,
    };
  }

  private applyServerErrors(fieldErrors: Readonly<Record<string, string>>): void {
    for (const [field, message] of Object.entries(fieldErrors)) {
      const control = this.form.get(field);
      if (control) {
        control.setErrors({ ...control.errors, server: message }, { emitEvent: false });
        control.markAsTouched();
      }
    }
  }

  private clearServerErrors(): void {
    for (const control of Object.values(this.form.controls)) {
      const errors = control.errors;
      if (!errors?.['server']) {
        continue;
      }
      const remaining = { ...errors };
      delete remaining['server'];
      control.setErrors(Object.keys(remaining).length > 0 ? remaining : null, { emitEvent: false });
    }
  }
}
