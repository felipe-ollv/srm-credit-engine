import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ExchangeRateResponse } from '../../core/http/api.models';
import { RUNTIME_CONFIG } from '../../core/config/runtime-config';

@Injectable()
export class CurrencyApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${inject(RUNTIME_CONFIG).apiBaseUrl}/api/v1/exchange-rates`;

  current(): Observable<ExchangeRateResponse> {
    return this.http.get<ExchangeRateResponse>(`${this.url}/current`, {
      params: new HttpParams().set('baseCurrency', 'USD').set('quoteCurrency', 'BRL'),
    });
  }

  refresh(): Observable<ExchangeRateResponse> {
    return this.http.post<ExchangeRateResponse>(`${this.url}/refresh`, null);
  }
}
