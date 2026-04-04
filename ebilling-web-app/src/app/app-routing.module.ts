import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { WelcomeComponent } from './pages/welcome/welcome.component';
import { Not403Component } from './pages/error/not403/not403.component';
import { Not404Component } from './pages/error/not404/not404.component';
import { ResetPasswordComponent } from './pages/login/reset-password/reset-password.component';
import { TokenComponent } from './pages/login/reset-password/token/token.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { CanActiveUserGuard } from './_service/canActivate/can-active-user.guard';
import { SessionValidationGuard } from './_service/canActivate/session-validation.guard';
import { PermissionGuard } from './_guard/permission.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [SessionValidationGuard],
  },
  { path: 'not-403', component: Not403Component },

  // Rutas para reseteo de contraseña
  {
    path: 'reset-password',
    component: ResetPasswordComponent,
    children: [{ path: ':token', component: TokenComponent }],
  },
  {
    path: 'welcome',
    component: WelcomeComponent,
    canActivate: [SessionValidationGuard],
    // No requiere validación de feature - accesible para todos los usuarios autenticados
  },

  // ✅ REST Convention: Plural resources
  // Todas las rutas protegidas usan SessionValidationGuard para validar
  // sesión (IP + Browser + Token + Punto de emisión)
  {
    path: 'clients',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/customer/customer.module').then((m) => m.CustomerModule),
  },
  {
    path: 'products',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/product/product.module').then((m) => m.ProductModule),
  },
  {
    path: 'users',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/user/user.module').then((m) => m.UserModule),
  },
  {
    path: 'companies',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/company/company.module').then((m) => m.CompanyModule),
  },
  {
    path: 'invoices',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/billing/billing.module').then((m) => m.BillingModule),
  },
  {
    path: 'delivery-guides',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/guides/guides.module').then((m) => m.GuidesModule),
  },
  {
    path: 'carriers',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/carrier/carrier.module').then((m) => m.CarrierModule),
  },

  // Rutas independientes de sub-recursos (REST convention)
  {
    path: 'establishments',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/establishment/establishment.module').then(
        (m) => m.EstablishmentModule
      ),
  },
  {
    path: 'invoice-points',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/invoice-point/invoice-point.module').then(
        (m) => m.InvoicePointModule
      ),
  },
  {
    path: 'company-users',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/company/company-user/company-user.module').then(
        (m) => m.CompanyUserModule
      ),
  },
  {
    path: 'proforma',
    redirectTo: '/invoices/proformas',
    pathMatch: 'full',
  },
  {
    path: 'drafts',
    redirectTo: '/invoices/drafts',
    pathMatch: 'full',
  },
  {
    path: 'credit-note',
    redirectTo: '/invoices/credit-notes',
    pathMatch: 'full',
  },
  {
    path: 'debit-note',
    redirectTo: '/invoices/debit-note',
    pathMatch: 'full',
  },
  {
    path: 'debit-note',
    redirectTo: '/invoices/debit-notes',
    pathMatch: 'full',
  },

  // Administración Global (Super Admin)
  {
    path: 'admin',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/admin/admin.module').then((m) => m.AdminModule),
  },

  // Configuración (singular - acción/singleton)
  {
    path: 'setting',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/setting/setting.module').then((m) => m.SettingModule),
  },
  {
    path: 'reports',
    canActivate: [SessionValidationGuard],
    loadChildren: () =>
      import('./pages/reports/reports.module').then((m) => m.ReportsModule),
  },

  // Rutas públicas - Consulta de Facturas (flujo completo)
  {
    path: 'invoice-consultation',
    loadChildren: () =>
      import(
        './pages/billing/invoice-consultation-entry/invoice-consultation-entry.module'
      ).then((m) => m.InvoiceConsultationEntryModule),
  },
  {
    path: 'invoice-consultation/validate',
    loadChildren: () =>
      import('./pages/billing/token-validation/token-validation.module').then(
        (m) => m.TokenValidationModule
      ),
  },
  {
    path: 'invoice-consultation/view',
    loadChildren: () =>
      import('./pages/billing/viewing-invoices/viewing-invoices.module').then(
        (m) => m.ViewingInvoicesModule
      ),
  },
  { path: '**', component: Not404Component },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
