import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { KEYCLOAK } from './keycloak.token';

describe('AuthService', () => {
  let keycloak: Keycloak;

  beforeEach(() => {
    keycloak = {
      token: 'access-token',
      tokenParsed: {
        preferred_username: 'operator',
        name: 'Operador SRM',
        realm_access: { roles: ['default-roles', 'operator'] },
      },
      updateToken: vi.fn().mockResolvedValue(false),
      clearToken: vi.fn(),
      login: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn().mockResolvedValue(undefined),
    } as unknown as Keycloak;

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: KEYCLOAK, useValue: keycloak },
      ],
    });
  });

  it('extracts and normalizes application roles from realm_access', () => {
    const service = TestBed.inject(AuthService);

    expect(service.displayName()).toBe('Operador SRM');
    expect(service.applicationRoles()).toEqual(['OPERATOR']);
    expect(service.hasApplicationRole()).toBe(true);
  });

  it('refreshes the in-memory token before returning it', async () => {
    const service = TestBed.inject(AuthService);

    await expect(service.freshAccessToken()).resolves.toBe('access-token');
    expect(keycloak.updateToken).toHaveBeenCalledWith(30);
  });
});
