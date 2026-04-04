import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// Angular Material
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';

// Routing
import { RolePermissionsRoutingModule } from './role-permissions-routing.module';

// Components
import { RolePermissionsContainerComponent } from './role-permissions-container/role-permissions-container.component';
import { PermissionMatrixComponent } from './permission-matrix/permission-matrix.component';
import { PlanPermissionMatrixComponent } from './plan-permission-matrix/plan-permission-matrix.component';

@NgModule({
  declarations: [
    RolePermissionsContainerComponent,
    PermissionMatrixComponent,
    PlanPermissionMatrixComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RolePermissionsRoutingModule,
    // Material
    MatTableModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatToolbarModule,
    MatTabsModule,
  ],
})
export class RolePermissionsModule {}
