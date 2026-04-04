import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// Angular CDK
import { CdkTableModule } from '@angular/cdk/table';

// Material Module
import { MaterialModule } from '../../../material/material.module';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

// Routing
import { MenusRoutingModule } from './menus-routing.module';

// Components
import { MenuListComponent } from './menu-list/menu-list.component';
import { MenuFormDialogComponent } from './menu-form-dialog/menu-form-dialog.component';

// Shared
import { SharedModule } from '../../../_shared/shared.module';

@NgModule({
  declarations: [MenuListComponent, MenuFormDialogComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MenusRoutingModule,
    CdkTableModule, // Necesario para mat-table
    MaterialModule, // Incluye todos los módulos de Material
    MatSlideToggleModule, // Toggle para activar/desactivar menús
    SharedModule,
  ],
})
export class MenusModule {}
