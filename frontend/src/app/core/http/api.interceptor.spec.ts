import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { KEYCLOAK } from '../auth/keycloak.token';
import { RUNTIME_CONFIG, RuntimeConfig } from '../config/runtime-config';
import { apiInterceptor, isApiRequest } from './api.interceptor';

describe('apiInterceptor', () => {
  const config: RuntimeConfig = {
    apiBaseUrl: 'http://localhost:8080',
    keycloakUrl: 'http://localhost:8081',
    keycloakRealm: 'srm-credit-engine',
    keycloakClientId: 'srm-credit-engine-web',
  };
  let http: HttpClient;
  let controller: HttpTestingController;
  let keycloak: Keycloak;

  beforeEach(() => {
    keycloak = {
      token: 'fresh-token',
      tokenParsed: { realm_access: { roles: ['OPERATOR'] } },
      updateToken: vi.fn().mockResolvedValue(true),
      clearToken: vi.fn(),
      login: vi.fn().mockResolvedValue(undefined),
    } as unknown as Keycloak;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
        AuthService,
        { provide: RUNTIME_CONFIG, useValue: config },
        { provide: KEYCLOAK, useValue: keycloak },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      controller.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('adds bearer and correlation headers only to the configured API origin', async () => {
    http.get('http://localhost:8080/api/v1/test').subscribe();
    await new Promise((resolve) => setTimeout(resolve, 0));

    const apiRequest = controller.expectOne('http://localhost:8080/api/v1/test');
    expect(apiRequest.request.headers.get('Authorization')).toBe('Bearer fresh-token');
    expect(apiRequest.request.headers.get('X-Correlation-Id')).toMatch(/^[0-9a-f-]{36}$/);
    apiRequest.flush({});

    http.get('https://external.example/data').subscribe();
    const externalRequest = controller.expectOne('https://external.example/data');
    expect(externalRequest.request.headers.has('Authorization')).toBe(false);
    externalRequest.flush({});
    expect(keycloak.updateToken).toHaveBeenCalledTimes(1);
  });

  it('does not trust URLs that merely start with the API host text', () => {
    expect(isApiRequest('http://localhost:8080/api/v1/test', config.apiBaseUrl)).toBe(true);
    expect(isApiRequest('http://localhost:8080.evil.example/test', config.apiBaseUrl)).toBe(false);
  });
});
