import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DateTimePtPipe, DecimalDisplayPipe } from '../pricing/pricing-display.pipe';
import { CurrencyApi } from './currency.api';
import { CurrencyFacade } from './currency.facade';

@Component({
  selector: 'app-currency',
  imports: [MatButtonModule, MatProgressBarModule, DateTimePtPipe, DecimalDisplayPipe],
  providers: [CurrencyApi, CurrencyFacade],
  template: `
    <main class="operations-page" aria-labelledby="currency-title">
      <header class="operations-header"><div><h1 id="currency-title">Administração cambial</h1><p>Atualize o snapshot USD/BRL usado pelo motor de crédito.</p></div><button mat-flat-button [disabled]="facade.loading()" (click)="facade.refresh()">Atualizar cotação</button></header>
      @if (facade.loading()) { <mat-progress-bar mode="indeterminate" aria-label="Atualizando cotação" /> }
      <section class="operations-panel rate-card" aria-live="polite">
        @if (facade.rate(); as rate) { <span>Cotação vigente</span><strong>1 {{ rate.baseCurrency }} = R$ {{ rate.rate | decimalDisplay }}</strong><dl><div><dt>Efetiva em</dt><dd>{{ rate.effectiveAt | dateTimePt }}</dd></div><div><dt>Capturada em</dt><dd>{{ rate.capturedAt | dateTimePt }}</dd></div></dl> }
        @else { <p>Nenhuma cotação vigente carregada.</p> }
      </section>
      @if (facade.failure(); as failure) { <p class="feedback error" role="alert">{{ failure.message }} @if (failure.correlationId) { <code>{{ failure.correlationId }}</code> }</p> }
    </main>
  `,
  styles: `.rate-card { display: grid; gap: .75rem; max-width: 42rem; } .rate-card > span { color: #62778b; } .rate-card > strong { color: #0f766e; font-size: clamp(2rem, 6vw, 3.5rem); } dl { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; } dt { color: #73879a; font-size: .75rem; } dd { margin: .25rem 0 0; color: #163b57; }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CurrencyComponent {
  readonly facade = inject(CurrencyFacade);
  constructor() { this.facade.load(); }
}
