import { Routes } from '@angular/router';
import { adminRoleGuard, applicationRoleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'simulacao' },
  {
    path: 'simulacao',
    canActivate: [applicationRoleGuard],
    loadComponent: () => import('./features/pricing/pricing-simulation.component')
      .then((module) => module.PricingSimulationComponent),
  },
  {
    path: 'cadastros/cedentes',
    canActivate: [applicationRoleGuard],
    loadComponent: () => import('./features/assignors/assignors.component')
      .then((module) => module.AssignorsComponent),
  },
  {
    path: 'cadastros/recebiveis',
    canActivate: [applicationRoleGuard],
    loadComponent: () => import('./features/receivables/receivables.component')
      .then((module) => module.ReceivablesComponent),
  },
  {
    path: 'liquidacao',
    canActivate: [applicationRoleGuard],
    loadComponent: () => import('./features/settlements/settlements.component')
      .then((module) => module.SettlementsComponent),
  },
  {
    path: 'extrato',
    canActivate: [applicationRoleGuard],
    loadComponent: () => import('./features/reporting/reporting.component')
      .then((module) => module.ReportingComponent),
  },
  {
    path: 'cambio',
    canActivate: [adminRoleGuard],
    loadComponent: () => import('./features/currency/currency.component')
      .then((module) => module.CurrencyComponent),
  },
  {
    path: 'sem-acesso',
    loadComponent: () => import('./no-access/no-access.component')
      .then((module) => module.NoAccessComponent),
  },
  { path: '**', redirectTo: 'simulacao' },
];
