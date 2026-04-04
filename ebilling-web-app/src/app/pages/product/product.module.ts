import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ProductComponent } from './product.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { SharedModule } from '../../_shared/shared.module';
import { ProductDialogModule } from './product-dialog.module';

const routes: Routes = [
  {
    path: '',
    component: ProductComponent,
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'PRODUCT_MANAGEMENT' },
  },
];

/**
 * Módulo de Productos (Lazy Loaded)
 *
 * Carga solo cuando se navega a /product
 * Usa modales para crear/editar productos en lugar de rutas hijas
 */
@NgModule({
  declarations: [ProductComponent],
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
    ProductDialogModule, // Importar para usar UpdateProductComponent
  ],
})
export class ProductModule {}
