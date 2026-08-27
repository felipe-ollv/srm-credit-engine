import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { bootstrapApplication } from '@angular/platform-browser';
import Keycloak from 'keycloak-js';
import { AppComponent } from './app/app.component';
import { createAppConfig } from './app/app.config';
import { loadRuntimeConfig } from './app/core/config/runtime-config';

async function bootstrap(): Promise<void> {
  registerLocaleData(localePt);
  const runtimeConfig = await loadRuntimeConfig();
  const keycloak = new Keycloak({
    url: runtimeConfig.keycloakUrl,
    realm: runtimeConfig.keycloakRealm,
    clientId: runtimeConfig.keycloakClientId,
  });

  await keycloak.init({
    onLoad: 'login-required',
    flow: 'standard',
    pkceMethod: 'S256',
  });

  await bootstrapApplication(
    AppComponent,
    createAppConfig(runtimeConfig, keycloak),
  );
}

bootstrap().catch((error: unknown) => {
  console.error('Unable to start SRM Credit Engine web application', error);
  document.body.innerHTML = `
    <main style="font-family:system-ui;max-width:42rem;margin:8rem auto;padding:2rem">
      <h1>Não foi possível iniciar a aplicação</h1>
      <p>Verifique a configuração e a disponibilidade do serviço de identidade.</p>
    </main>`;
});
