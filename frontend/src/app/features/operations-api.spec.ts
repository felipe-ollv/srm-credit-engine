import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { RUNTIME_CONFIG, RuntimeConfig } from '../core/config/runtime-config';
import { AssignorsApi } from './assignors/assignors.api';
import { CurrencyApi } from './currency/currency.api';
import { ReceivablesApi } from './receivables/receivables.api';
import { ReportingApi } from './reporting/reporting.api';
import { SettlementsApi } from './settlements/settlements.api';

describe('operations API contracts', () => {
  const config: RuntimeConfig = {
    apiBaseUrl: 'http://localhost:8080', keycloakUrl: '', keycloakRealm: '', keycloakClientId: '',
  };
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(),
      AssignorsApi, ReceivablesApi, SettlementsApi, ReportingApi, CurrencyApi,
      { provide: RUNTIME_CONFIG, useValue: config },
    ] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });

  it('sends assignor search pagination and query', () => {
    TestBed.inject(AssignorsApi).search('indústria', 2, 20).subscribe();
    const request = http.expectOne((candidate) => candidate.url.endsWith('/api/v1/assignors'));
    expect(request.request.params.get('query')).toBe('indústria');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('filters available receivables for settlement', () => {
    TestBed.inject(SettlementsApi).availableReceivables().subscribe();
    const request = http.expectOne((candidate) => candidate.url.endsWith('/api/v1/receivables'));
    expect(request.request.params.get('status')).toBe('AVAILABLE');
    expect(request.request.params.get('size')).toBe('100');
    request.flush({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 });
  });

  it('sends the idempotency key with the batch', () => {
    const body = { items: [{ receivableId: 'id', paymentCurrency: 'BRL' as const }] };
    TestBed.inject(SettlementsApi).settle(body, 'attempt-key').subscribe();
    const request = http.expectOne('http://localhost:8080/api/v1/settlement-batches');
    expect(request.request.headers.get('Idempotency-Key')).toBe('attempt-key');
    expect(request.request.body).toEqual(body);
    request.flush({ batchId: 'b', status: 'COMPLETED', requestedAt: '', completedAt: '', items: [] });
  });

  it('maps reporting filters and server-side sort to query parameters', () => {
    TestBed.inject(ReportingApi).search({
      from: '2026-08-01', to: '2026-08-31', assignorId: 'a', paymentCurrency: 'USD', sort: 'paymentAmount,desc',
    }, 3).subscribe();
    const request = http.expectOne((candidate) => candidate.url.endsWith('/api/v1/settlements'));
    expect(request.request.params.get('from')).toBe('2026-08-01');
    expect(request.request.params.get('paymentCurrency')).toBe('USD');
    expect(request.request.params.get('sort')).toBe('paymentAmount,desc');
    expect(request.request.params.get('page')).toBe('3');
    request.flush({ content: [], page: 3, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('uses the fixed USD/BRL pair and POST refresh contract', () => {
    const api = TestBed.inject(CurrencyApi);
    api.current().subscribe();
    const current = http.expectOne((candidate) => candidate.url.endsWith('/api/v1/exchange-rates/current'));
    expect(current.request.params.get('baseCurrency')).toBe('USD');
    expect(current.request.params.get('quoteCurrency')).toBe('BRL');
    current.flush({ baseCurrency: 'USD', quoteCurrency: 'BRL', rate: '5.4321', effectiveAt: '', capturedAt: '' });
    api.refresh().subscribe();
    expect(http.expectOne('http://localhost:8080/api/v1/exchange-rates/refresh').request.method).toBe('POST');
  });
});
