import { HttpErrorResponse } from '@angular/common/http';
import { ApiFailure, ApiProblem } from './api.models';

export function toApiFailure(error: unknown, fallback: string): ApiFailure {
  if (error instanceof HttpErrorResponse && isApiProblem(error.error)) {
    return {
      code: error.error.code,
      message: error.error.detail || fallback,
      correlationId: error.error.correlationId || null,
      fieldErrors: error.error.fieldErrors ?? {},
    };
  }
  return {
    code: 'NETWORK_ERROR',
    message: fallback,
    correlationId: null,
    fieldErrors: {},
  };
}

function isApiProblem(value: unknown): value is ApiProblem {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<ApiProblem>;
  return typeof candidate.code === 'string'
    && typeof candidate.detail === 'string'
    && typeof candidate.correlationId === 'string';
}
