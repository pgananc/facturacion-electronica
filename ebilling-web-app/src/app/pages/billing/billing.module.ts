import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { BillingComponent } from './billing.component';
import { DraftInvoicesComponent } from './draft-invoices/draft-invoices.component';
import { CreditNotesComponent } from './credit-notes/credit-notes.component';
import { SelectInvoiceDialogComponent } from './credit-notes/select-invoice-dialog/select-invoice-dialog.component';
import { CreateCreditNoteComponent } from './credit-notes/create-credit-note/create-credit-note.component';
import { DebitNotesComponent } from './debit-notes/debit-notes.component';
import { CreateDebitNoteComponent } from './debit-notes/create-debit-note/create-debit-note.component';
import { InvoiceListComponent } from './invoice-list/invoice-list.component';
import { SelectProductDialogComponent } from './select-product-dialog/select-product-dialog.component';
import { AddPaymentDialogComponent } from './add-payment-dialog/add-payment-dialog.component';
import { TicketPrintComponent } from './ticket-print/ticket-print.component';
import { SuccessInvoiceDialogComponent } from './success-invoice-dialog/success-invoice-dialog.component';
import { SuccessDebitNoteDialogComponent } from './debit-notes/success-debit-note-dialog/success-debit-note-dialog.component';
import { DebitNoteTicketPrintComponent } from './debit-notes/debit-note-ticket-print/debit-note-ticket-print.component';
import { ProformasComponent } from './proformas/proformas.component';
import { CreateProformaComponent } from './proformas/create-proforma/create-proforma.component';
import { SharedModule } from '../../_shared/shared.module';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductDialogModule } from '../product/product-dialog.module';

const routes: Routes = [
  {
    path: 'proformas/create',
    component: CreateProformaComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'INVOICE_CREATE' },
  },
  {
    path: 'proformas/edit/:id',
    component: CreateProformaComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'INVOICE_CREATE' },
  },
  {
    path: 'proformas',
    component: ProformasComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'INVOICE_CREATE' },
  },
  {
    path: 'drafts',
    component: DraftInvoicesComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'INVOICE_CREATE' },
  },
  {
    path: 'create',
    component: BillingComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'INVOICE_CREATE' },
  },
  {
    path: 'credit-notes',
    component: CreditNotesComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'CREDIT_NOTE_CREATE' },
  },
  {
    path: 'debit-note',
    component: DebitNotesComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'debit-notes',
    component: CreditNotesComponent, // TODO: Crear DebitNotesComponent
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'DEBIT_NOTE_CREATE' },
  },
  {
    path: 'ticket/:invoiceId',
    component: TicketPrintComponent,
    // ✅ NO requiere autenticación - permite acceso directo a la URL
    // El componente manejará los errores de forma apropiada
  },
  {
    path: 'debit-note-ticket/:debitNoteId',
    component: DebitNoteTicketPrintComponent,
    // ✅ NO requiere autenticación - permite acceso directo a la URL
  },
  {
    path: '',
    component: InvoiceListComponent,
    canActivate: [CanActiveUserGuard],
  },
];

/**
 * Módulo de Facturación (Lazy Loaded)
 *
 * Este módulo se carga solo cuando el usuario navega a /billing
 * Beneficios:
 * - Reduce el tamaño del bundle inicial
 * - Mejora el tiempo de carga inicial
 * - Aísla las dependencias del módulo de facturación
 */
@NgModule({
  declarations: [
    BillingComponent,
    DraftInvoicesComponent,
    InvoiceListComponent,
    CreditNotesComponent,
    SelectProductDialogComponent,
    AddPaymentDialogComponent,
    SelectInvoiceDialogComponent,
    CreateCreditNoteComponent,
    DebitNotesComponent,
    CreateDebitNoteComponent,
    TicketPrintComponent,
    SuccessInvoiceDialogComponent,
    SuccessDebitNoteDialogComponent,
    DebitNoteTicketPrintComponent,
    ProformasComponent,
    CreateProformaComponent,
  ],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    SharedModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ProductDialogModule, // Importar solo el módulo de diálogo sin rutas
  ],
})
export class BillingModule {}
