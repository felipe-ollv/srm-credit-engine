import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../core/http/api.models';
import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import { AssignorOption } from '../receivables/receivables.models';
import { ReportingFilters, SettlementPage } from './reporting.models';

@Injectable()
export class ReportingApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;

  search(filters: ReportingFilters, page: number, size = 20): Observable<SettlementPage> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', filters.sort);
    for (const [name, value] of Object.entries(filters)) {
      if (name !== 'sort' && value) params = params.set(name, value);
    }
    return this.http.get<SettlementPage>(`${this.base}/api/v1/settlements`, { params });
  }

  assignors(): Observable<PageResponse<AssignorOption>> {
    return this.http.get<PageResponse<AssignorOption>>(`${this.base}/api/v1/assignors`, {
      params: new HttpParams().set('page', 0).set('size', 100),
    });
  }
}
