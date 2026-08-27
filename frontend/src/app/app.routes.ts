import { Routes } from '@angular/router';
import { applicationRoleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'simulacao' },
  {
    path: 'simulacao',
    canActivate: [applicationRoleGuard],
    loadComponent: () => import('./features/pricing/pricing-simulation.component')
      .then((module) => module.PricingSimulationComponent),
  },
  {
    path: 'sem-acesso',
    loadComponent: () => import('./no-access/no-access.component')
      .then((module) => module.NoAccessComponent),
  },
  { path: '**', redirectTo: 'simulacao' },
];
