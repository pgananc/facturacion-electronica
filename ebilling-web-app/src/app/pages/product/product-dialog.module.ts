import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UpdateProductComponent } from './update-product/update-product.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { SharedModule } from '../../_shared/shared.module';

/**
 * ProductDialogModule - Módulo solo para el componente de diálogo de productos
 * 
 * Este módulo exporta UpdateProductComponent SIN rutas, para poder usarlo
 * en otros módulos (como BillingModule) sin causar conflictos de routing.
 * 
 * NO incluye RouterModule.forChild() para evitar problemas de routing.
 */
@NgModule({
  declarations: [UpdateProductComponent],
  imports: [
    CommonModule,
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatDatepickerModule,
    MatNativeDateModule,
    SharedModule,
  ],
  exports: [UpdateProductComponent], // Exportar para usar en otros módulos
})
export class ProductDialogModule {}


