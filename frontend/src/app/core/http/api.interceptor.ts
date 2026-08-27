import { inject } from '@angular/core';
import {
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { RUNTIME_CONFIG } from '../config/runtime-config';

export const apiInterceptor: HttpInterceptorFn = (request, next) => {
  const config = inject(RUNTIME_CONFIG);
  const auth = inject(AuthService);
  if (!isApiRequest(request.url, config.apiBaseUrl)) {
    return next(request);
  }

  return from(auth.freshAccessToken()).pipe(
    switchMap((token) => next(request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        'X-Correlation-Id': crypto.randomUUID(),
      },
    }))),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        void auth.reauthenticate();
      }
      return throwError(() => error);
    }),
  );
};

export function isApiRequest(requestUrl: string, apiBaseUrl: string): boolean {
  try {
    const base = new URL(apiBaseUrl);
    const candidate = new URL(requestUrl, window.location.origin);
    const basePath = base.pathname.replace(/\/$/, '');
    return candidate.origin === base.origin
      && (basePath === '' || candidate.pathname === basePath || candidate.pathname.startsWith(`${basePath}/`));
  } catch {
    return false;
  }
}
