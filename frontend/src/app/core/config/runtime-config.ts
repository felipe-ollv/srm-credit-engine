import { InjectionToken } from '@angular/core';

export interface RuntimeConfig {
  readonly apiBaseUrl: string;
  readonly keycloakUrl: string;
  readonly keycloakRealm: string;
  readonly keycloakClientId: string;
}

export const RUNTIME_CONFIG = new InjectionToken<RuntimeConfig>('runtime.config');

export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  const response = await fetch('/config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Unable to load runtime configuration (${response.status})`);
  }

  const candidate = (await response.json()) as Partial<RuntimeConfig>;
  const config: RuntimeConfig = {
    apiBaseUrl: requiredUrl(candidate.apiBaseUrl, 'apiBaseUrl'),
    keycloakUrl: requiredUrl(candidate.keycloakUrl, 'keycloakUrl'),
    keycloakRealm: requiredText(candidate.keycloakRealm, 'keycloakRealm'),
    keycloakClientId: requiredText(candidate.keycloakClientId, 'keycloakClientId'),
  };
  return Object.freeze(config);
}

function requiredUrl(value: string | undefined, field: string): string {
  const normalized = requiredText(value, field).replace(/\/$/, '');
  new URL(normalized);
  return normalized;
}

function requiredText(value: string | undefined, field: string): string {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(`Runtime configuration field ${field} is required`);
  }
  return value.trim();
}
