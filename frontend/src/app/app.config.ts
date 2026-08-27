import {
  ApplicationConfig,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import Keycloak from 'keycloak-js';
import { routes } from './app.routes';
import { KEYCLOAK } from './core/auth/keycloak.token';
import { apiInterceptor } from './core/http/api.interceptor';
import { RUNTIME_CONFIG, RuntimeConfig } from './core/config/runtime-config';

export function createAppConfig(
  runtimeConfig: RuntimeConfig,
  keycloak: Keycloak,
): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([apiInterceptor])),
      provideAnimationsAsync(),
      { provide: LOCALE_ID, useValue: 'pt-BR' },
      { provide: RUNTIME_CONFIG, useValue: runtimeConfig },
      { provide: KEYCLOAK, useValue: keycloak },
    ],
  };
}
