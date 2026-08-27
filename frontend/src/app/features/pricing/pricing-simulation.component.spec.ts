import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RUNTIME_CONFIG, RuntimeConfig } from '../../core/config/runtime-config';
import { addCalendarMonths, pricingDateBounds } from './date.util';
import { PricingSimulationComponent } from './pricing-simulation.component';
import { PricingSimulationResponse } from './pricing.models';

describe('PricingSimulationComponent', () => {
  const config: RuntimeConfig = {
    apiBaseUrl: 'http://localhost:8080',
    keycloakUrl: 'http://localhost:8081',
    keycloakRealm: 'srm-credit-engine',
    keycloakClientId: 'srm-credit-engine-web',
  };
  let fixture: ComponentFixture<PricingSimulationComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [PricingSimulationComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: RUNTIME_CONFIG, useValue: config },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PricingSimulationComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    fixture.destroy();
    TestBed.resetTestingModule();
    vi.useRealTimers();
  });

  it('sends normalized decimal strings and renders a BRL result without FX', async () => {
    const dueDate = addCalendarMonths(pricingDateBounds().minimum, 3);
    fixture.componentInstance.form.setValue({
      receivableType: 'DUPLICATA_MERCANTIL',
      faceValue: '100.000,00',
      dueDate,
      paymentCurrency: 'BRL',
    });
    await vi.advanceTimersByTimeAsync(400);

    const request = http.expectOne('http://localhost:8080/api/v1/pricing/simulations');
    expect(request.request.body).toEqual({
      receivableType: 'DUPLICATA_MERCANTIL',
      faceValue: '100000.00',
      dueDate,
      paymentCurrency: 'BRL',
    });
    request.flush(response('BRL'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('R$ 92.859,94');
    expect(fixture.nativeElement.textContent).not.toContain('Cotação aplicada');
  });

  it('renders USD exchange-rate details', async () => {
    const dueDate = addCalendarMonths(pricingDateBounds().minimum, 3);
    fixture.componentInstance.form.setValue({
      receivableType: 'DUPLICATA_MERCANTIL',
      faceValue: '100.000,00',
      dueDate,
      paymentCurrency: 'USD',
    });
    await vi.advanceTimersByTimeAsync(400);

    http.expectOne('http://localhost:8080/api/v1/pricing/simulations').flush(response('USD'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('US$ 17.094,67');
    expect(fixture.nativeElement.textContent).toContain('1 USD = R$ 5,4321');
  });

  it('associates backend field errors with the corresponding control', async () => {
    const dueDate = addCalendarMonths(pricingDateBounds().minimum, 3);
    fixture.componentInstance.form.setValue({
      receivableType: 'DUPLICATA_MERCANTIL',
      faceValue: '100.000,00',
      dueDate,
      paymentCurrency: 'BRL',
    });
    await vi.advanceTimersByTimeAsync(400);

    http.expectOne('http://localhost:8080/api/v1/pricing/simulations').flush({
      status: 400,
      title: 'Bad Request',
      detail: 'Request validation failed',
      instance: '/api/v1/pricing/simulations',
      code: 'REQUEST_INVALID',
      correlationId: 'request-456',
      fieldErrors: { faceValue: 'invalid face value' },
    }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.componentInstance.faceValue.getError('server')).toBe('invalid face value');
    expect(fixture.nativeElement.textContent).toContain('request-456');
  });
});

function response(currency: 'BRL' | 'USD'): PricingSimulationResponse {
  return {
    receivableType: 'DUPLICATA_MERCANTIL',
    faceValue: { amount: '100000.00', currency: 'BRL' },
    presentValue: { amount: '92859.94', currency: 'BRL' },
    discount: { amount: '7140.06', currency: 'BRL' },
    payment: currency === 'BRL'
      ? { amount: '92859.94', currency: 'BRL' }
      : { amount: '17094.67', currency: 'USD' },
    termMonths: 3,
    baseRate: '0.01',
    spread: '0.015',
    exchangeRate: currency === 'BRL' ? null : {
      baseCurrency: 'USD',
      quoteCurrency: 'BRL',
      rate: '5.4321',
      effectiveAt: '2026-08-26T19:00:00Z',
      capturedAt: '2026-08-26T19:00:00Z',
    },
    pricingDate: '2026-08-26',
    calculatedAt: '2026-08-26T19:00:00Z',
  };
}
