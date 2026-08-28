import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import { AssignorsComponent } from './assignors.component';

describe('AssignorsComponent', () => {
  let fixture: ComponentFixture<AssignorsComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssignorsComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
        { provide: RUNTIME_CONFIG, useValue: { apiBaseUrl: 'http://localhost:8080' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AssignorsComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    flushPage(http);
  });

  afterEach(() => { http.verify(); fixture.destroy(); TestBed.resetTestingModule(); });

  it('rejects a CNPJ with invalid check digits before calling the API', () => {
    fixture.componentInstance.form.setValue({ document: '11.222.333/0001-82', legalName: 'Cedente Inválido' });
    fixture.componentInstance.submit();
    expect(fixture.componentInstance.document.hasError('invalidCnpj')).toBe(true);
    http.expectNone('http://localhost:8080/api/v1/assignors');
  });

  it('normalizes a valid CNPJ and reloads the first page after creation', () => {
    fixture.componentInstance.form.setValue({ document: '11.222.333/0001-81', legalName: 'Cedente Teste' });
    fixture.componentInstance.submit();
    const create = http.expectOne('http://localhost:8080/api/v1/assignors');
    expect(create.request.body).toEqual({ document: '11222333000181', legalName: 'Cedente Teste' });
    create.flush({ id: 'a', document: '11222333000181', legalName: 'Cedente Teste', createdAt: '2026-08-28T00:00:00Z' });
    flushPage(http);
    expect(fixture.componentInstance.facade.saved()).toBe(true);
  });
});

function flushPage(http: HttpTestingController): void {
  http.expectOne((request) => request.url === 'http://localhost:8080/api/v1/assignors' && request.method === 'GET')
    .flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
}
