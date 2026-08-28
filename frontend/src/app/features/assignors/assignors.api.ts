import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import { AssignorPage, AssignorResponse, CreateAssignorRequest } from './assignors.models';

@Injectable()
export class AssignorsApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RUNTIME_CONFIG);
  private readonly url = `${this.config.apiBaseUrl}/api/v1/assignors`;

  create(request: CreateAssignorRequest): Observable<AssignorResponse> {
    return this.http.post<AssignorResponse>(this.url, request);
  }

  search(query: string, page = 0, size = 20): Observable<AssignorPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query.trim()) {
      params = params.set('query', query.trim());
    }
    return this.http.get<AssignorPage>(this.url, { params });
  }
}
