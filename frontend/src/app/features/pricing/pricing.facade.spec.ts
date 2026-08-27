import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { PricingApi } from './pricing.api';
import { PricingFacade } from './pricing.facade';
import {
  PricingSimulationRequest,
  PricingSimulationResponse,
} from './pricing.models';

describe('PricingFacade', () => {
  const command: PricingSimulationRequest = {
    receivableType: 'DUPLICATA_MERCANTIL',
    faceValue: '100000.00',
    dueDate: '2026-11-26',
    paymentCurrency: 'BRL',
  };
  const response: PricingSimulationResponse = {
    receivableType: 'DUPLICATA_MERCANTIL',
    faceValue: { amount: '100000.00', currency: 'BRL' },
    presentValue: { amount: '92859.94', currency: 'BRL' },
    discount: { amount: '7140.06', currency: 'BRL' },
    payment: { amount: '92859.94', currency: 'BRL' },
    termMonths: 3,
    baseRate: '0.01',
    spread: '0.015',
    exchangeRate: null,
    pricingDate: '2026-08-26',
    calculatedAt: '2026-08-26T19:00:00Z',
  };
  let commands: Subject<PricingSimulationRequest | null>;
  let api: { simulate: ReturnType<typeof vi.fn> };
  let facade: PricingFacade;

  beforeEach(() => {
    vi.useFakeTimers();
    commands = new Subject();
    api = { simulate: vi.fn((): Observable<PricingSimulationResponse> => of(response)) };
    TestBed.configureTestingModule({
      providers: [
        PricingFacade,
        { provide: PricingApi, useValue: api },
      ],
    });
    facade = TestBed.inject(PricingFacade);
    facade.connect(commands);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.useRealTimers();
  });

  it('debounces and ignores consecutive duplicate commands', async () => {
    commands.next(command);
    await vi.advanceTimersByTimeAsync(399);
    expect(api.simulate).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1);
    expect(api.simulate).toHaveBeenCalledOnce();
    expect(facade.result()).toEqual(response);

    commands.next({ ...command });
    await vi.advanceTimersByTimeAsync(400);
    expect(api.simulate).toHaveBeenCalledOnce();
  });

  it('cancels a request when a newer valid command arrives', async () => {
    const firstResponse = new Subject<PricingSimulationResponse>();
    api.simulate
      .mockReturnValueOnce(firstResponse)
      .mockReturnValueOnce(of({ ...response, payment: { amount: '17094.67', currency: 'USD' } }));

    commands.next(command);
    await vi.advanceTimersByTimeAsync(400);
    commands.next({ ...command, paymentCurrency: 'USD' });
    await vi.advanceTimersByTimeAsync(400);
    firstResponse.next(response);

    expect(facade.result()?.payment.currency).toBe('USD');
  });

  it('maps Problem Details and allows retrying the last command', async () => {
    api.simulate.mockReturnValueOnce(throwError(() => new HttpErrorResponse({
      status: 503,
      error: {
        status: 503,
        title: 'Service Unavailable',
        detail: 'No valid rate',
        instance: '/api/v1/pricing/simulations',
        code: 'FX_RATE_UNAVAILABLE',
        correlationId: 'request-123',
      },
    }))).mockReturnValueOnce(of(response));

    commands.next(command);
    await vi.advanceTimersByTimeAsync(400);
    expect(facade.failure()?.code).toBe('FX_RATE_UNAVAILABLE');
    expect(facade.failure()?.correlationId).toBe('request-123');

    facade.retry();
    expect(facade.result()).toEqual(response);
  });
});
