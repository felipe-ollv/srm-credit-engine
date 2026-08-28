import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../core/http/api.models';
import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import {
  AssignorOption,
  CreateReceivableRequest,
  ReceivablePage,
  ReceivableResponse,
  ReceivableStatus,
} from './receivables.models';

@Injectable()
export class ReceivablesApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;

  create(request: CreateReceivableRequest): Observable<ReceivableResponse> {
    return this.http.post<ReceivableResponse>(`${this.base}/api/v1/receivables`, request);
  }

  search(assignorId: string, status: ReceivableStatus | '', page = 0, size = 20): Observable<ReceivablePage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (assignorId) params = params.set('assignorId', assignorId);
    if (status) params = params.set('status', status);
    return this.http.get<ReceivablePage>(`${this.base}/api/v1/receivables`, { params });
  }

  assignors(): Observable<PageResponse<AssignorOption>> {
    return this.http.get<PageResponse<AssignorOption>>(`${this.base}/api/v1/assignors`, {
      params: new HttpParams().set('page', 0).set('size', 100),
    });
  }
}
