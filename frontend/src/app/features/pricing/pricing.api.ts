import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../../core/config/runtime-config';
import {
  PricingSimulationRequest,
  PricingSimulationResponse,
} from './pricing.models';

@Injectable()
export class PricingApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RUNTIME_CONFIG);

  simulate(request: PricingSimulationRequest): Observable<PricingSimulationResponse> {
    return this.http.post<PricingSimulationResponse>(
      `${this.config.apiBaseUrl}/api/v1/pricing/simulations`,
      request,
    );
  }
}
