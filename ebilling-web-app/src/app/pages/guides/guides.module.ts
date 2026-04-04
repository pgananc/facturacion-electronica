import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GuidesRoutingModule } from './guides-routing.module';
import { GuideFormComponent } from './guide-form/guide-form.component';
import { GuideListComponent } from './guide-list/guide-list.component';
import { GuideTicketPrintComponent } from './guide-ticket-print/guide-ticket-print.component';
import { SuccessGuideDialogComponent } from './success-guide-dialog/success-guide-dialog.component';
import { AddAdditionalFieldDialogComponent } from './add-additional-field-dialog/add-additional-field-dialog.component';
import { SharedModule } from '../../_shared/shared.module';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductDialogModule } from '../product/product-dialog.module';

/**
 * Módulo de Guías de Remisión (Lazy Loaded)
 *
 * Este módulo se carga solo cuando el usuario navega a /guides
 */
@NgModule({
  declarations: [GuideFormComponent, GuideListComponent, GuideTicketPrintComponent, SuccessGuideDialogComponent, AddAdditionalFieldDialogComponent],
  imports: [
    CommonModule,
    GuidesRoutingModule,
    SharedModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    ProductDialogModule,
  ],
})
export class GuidesModule {}
