import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const applicationRoleGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasApplicationRole()
    ? true
    : inject(Router).createUrlTree(['/sem-acesso']);
};

export const adminRoleGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.hasRole('ADMIN')
    ? true
    : inject(Router).createUrlTree(['/sem-acesso']);
};
