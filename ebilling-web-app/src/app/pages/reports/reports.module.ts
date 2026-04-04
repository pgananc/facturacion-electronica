import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../material/material.module';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { SharedModule } from '../../_shared/shared.module';

// Components
import { ReportsComponent } from './reports.component';
import { SalesByPeriodComponent } from './sales-by-period/sales-by-period.component';
import { SalesByClientComponent } from './sales-by-client/sales-by-client.component';
import { SalesByProductComponent } from './sales-by-product/sales-by-product.component';
import { CreditNotesReportComponent } from './credit-notes/credit-notes.component';
import { PendingInvoicesComponent } from './pending-invoices/pending-invoices.component';
import { SalesDashboardComponent } from './sales-dashboard/sales-dashboard.component';
import { BranchComparisonComponent } from './branch-comparison/branch-comparison.component';
import { PaymentMethodsReportComponent } from './payment-methods/payment-methods.component';

const routes: Routes = [
  {
    path: '',
    component: ReportsComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'sales-by-period',
    component: SalesByPeriodComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'sales-by-client',
    component: SalesByClientComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'sales-by-product',
    component: SalesByProductComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'credit-notes',
    component: CreditNotesReportComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'pending-invoices',
    component: PendingInvoicesComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'sales-dashboard',
    component: SalesDashboardComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'REPORT_ADVANCED' },
  },
  {
    path: 'branch-comparison',
    component: BranchComparisonComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'REPORT_ADVANCED' },
  },
  {
    path: 'payment-methods',
    component: PaymentMethodsReportComponent,
    canActivate: [CanActiveUserGuard],
  },
];

/**
 * Módulo de Reportes (Lazy Loaded)
 *
 * Este módulo se carga solo cuando el usuario navega a /reports
 * Incluye todos los reportes operativos y gerenciales
 */
@NgModule({
  declarations: [
    ReportsComponent,
    SalesByPeriodComponent,
    SalesByClientComponent,
    SalesByProductComponent,
    CreditNotesReportComponent,
    PendingInvoicesComponent,
    SalesDashboardComponent,
    BranchComparisonComponent,
    PaymentMethodsReportComponent,
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatDatepickerModule,
    MatNativeDateModule,
    SharedModule,
  ],
})
export class ReportsModule {}
