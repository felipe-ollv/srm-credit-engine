import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MatButtonModule, MatChipsModule, MatToolbarModule],
  template: `
    <header class="app-header">
      <mat-toolbar>
        <a class="brand" href="/simulacao" aria-label="SRM Credit Engine — início">
          <span class="brand-mark" aria-hidden="true">SRM</span>
          <span>
            <strong>Credit Engine</strong>
            <small>Mesa de operações</small>
          </span>
        </a>
        <span class="spacer"></span>
        <div class="identity">
          <span class="user">
            <strong>{{ auth.displayName() }}</strong>
            <small>{{ auth.username() }}</small>
          </span>
          <mat-chip-set aria-label="Perfis do usuário">
            @for (role of auth.applicationRoles(); track role) {
              <mat-chip>{{ role }}</mat-chip>
            }
          </mat-chip-set>
          <button mat-button type="button" (click)="logout()">Sair</button>
        </div>
      </mat-toolbar>
    </header>
    <router-outlet />
  `,
  styles: `
    .app-header { position: sticky; top: 0; z-index: 20; box-shadow: 0 1px 0 rgba(16, 42, 67, .12); }
    mat-toolbar { min-height: 4.5rem; padding: .5rem clamp(1rem, 4vw, 3rem); background: #102a43; color: white; }
    .brand { display: flex; align-items: center; gap: .8rem; color: inherit; text-decoration: none; }
    .brand-mark { display: grid; place-items: center; width: 2.6rem; height: 2.6rem; border-radius: .7rem; background: #14b8a6; color: #092f2b; font-weight: 900; font-size: .8rem; letter-spacing: .04em; }
    .brand strong, .brand small, .user strong, .user small { display: block; }
    .brand strong { font-size: 1rem; }
    .brand small, .user small { color: #c7d7e5; font-size: .72rem; }
    .spacer { flex: 1; }
    .identity { display: flex; align-items: center; gap: 1rem; }
    .user { text-align: right; line-height: 1.2; }
    mat-chip { --mdc-chip-label-text-color: #dffaf6; --mdc-chip-elevated-container-color: rgba(20, 184, 166, .2); font-size: .72rem; }
    button { color: white; }
    @media (max-width: 700px) {
      mat-toolbar { min-height: 4rem; }
      .brand small, .user, mat-chip-set { display: none; }
      .identity { gap: .25rem; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  readonly auth = inject(AuthService);

  logout(): void {
    void this.auth.logout();
  }
}
