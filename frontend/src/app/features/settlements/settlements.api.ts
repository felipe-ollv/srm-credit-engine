import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../core/http/api.models';
import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import { ReceivableResponse } from '../receivables/receivables.models';
import { SettlementBatchRequest, SettlementBatchResponse } from './settlements.models';

@Injectable()
export class SettlementsApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;

  availableReceivables(): Observable<PageResponse<ReceivableResponse>> {
    return this.http.get<PageResponse<ReceivableResponse>>(`${this.base}/api/v1/receivables`, {
      params: new HttpParams().set('status', 'AVAILABLE').set('page', 0).set('size', 100),
    });
  }

  settle(request: SettlementBatchRequest, idempotencyKey: string): Observable<SettlementBatchResponse> {
    return this.http.post<SettlementBatchResponse>(`${this.base}/api/v1/settlement-batches`, request, {
      headers: new HttpHeaders().set('Idempotency-Key', idempotencyKey),
    });
  }
}
