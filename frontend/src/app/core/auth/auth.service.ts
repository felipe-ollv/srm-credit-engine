import { computed, inject, Injectable } from '@angular/core';
import { KEYCLOAK } from './keycloak.token';

export type ApplicationRole = 'OPERATOR' | 'ADMIN';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(KEYCLOAK);

  readonly username = computed(() => this.claim('preferred_username') ?? 'Usuário');
  readonly displayName = computed(
    () => this.claim('name') ?? this.claim('preferred_username') ?? 'Usuário',
  );
  readonly roles = computed(() => this.readRoles());
  readonly applicationRoles = computed(() =>
    this.roles().filter((role): role is ApplicationRole =>
      role === 'OPERATOR' || role === 'ADMIN',
    ),
  );

  hasApplicationRole(): boolean {
    return this.applicationRoles().length > 0;
  }

  hasRole(role: ApplicationRole): boolean {
    return this.applicationRoles().includes(role);
  }

  async freshAccessToken(): Promise<string> {
    try {
      await this.keycloak.updateToken(30);
    } catch (error) {
      await this.reauthenticate();
      throw error;
    }

    if (!this.keycloak.token) {
      await this.reauthenticate();
      throw new Error('Keycloak did not provide an access token');
    }
    return this.keycloak.token;
  }

  async reauthenticate(): Promise<void> {
    this.keycloak.clearToken();
    await this.keycloak.login({ redirectUri: window.location.href });
  }

  async logout(): Promise<void> {
    await this.keycloak.logout({ redirectUri: window.location.origin });
  }

  private claim(name: string): string | null {
    const value = this.keycloak.tokenParsed?.[name];
    return typeof value === 'string' && value.trim() !== '' ? value : null;
  }

  private readRoles(): readonly string[] {
    const realmAccess = this.keycloak.tokenParsed?.['realm_access'];
    if (!isRecord(realmAccess) || !Array.isArray(realmAccess['roles'])) {
      return [];
    }
    return realmAccess['roles']
      .filter((role): role is string => typeof role === 'string')
      .map((role) => role.toUpperCase());
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
