import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-no-access',
  imports: [MatButtonModule],
  template: `
    <main class="no-access" aria-labelledby="no-access-title">
      <p class="eyebrow">Acesso restrito</p>
      <h1 id="no-access-title">Seu usuário não possui um perfil operacional</h1>
      <p>Solicite os perfis OPERATOR ou ADMIN para acessar o painel.</p>
      <button mat-flat-button type="button" (click)="logout()">Sair e trocar de usuário</button>
    </main>
  `,
  styles: `
    .no-access { max-width: 42rem; margin: 8rem auto; padding: 3rem; text-align: center; background: white; border-radius: 1.25rem; box-shadow: 0 1rem 3rem rgba(16, 42, 67, .1); }
    .eyebrow { color: #0f766e; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; }
    h1 { color: #102a43; font-size: clamp(1.75rem, 4vw, 2.5rem); }
    p { color: #52667a; line-height: 1.6; }
    button { margin-top: 1rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoAccessComponent {
  private readonly auth = inject(AuthService);

  logout(): void {
    void this.auth.logout();
  }
}
