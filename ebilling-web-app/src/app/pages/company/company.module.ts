import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { CompanyComponent } from './company.component';
import { UpdateCompanyComponent } from './update-company/update-company.component';
import { CompanyInfoComponent } from './company-info/company-info.component';
import { EstablishmentListComponent } from './establishment-list/establishment-list.component';
import { UpdateEstablishmentComponent } from './update-establishment/update-establishment.component';
import { IssuancePointListComponent } from './issuance-point-list/issuance-point-list.component';
import { UpdateIssuancePointComponent } from './update-issuance-point/update-issuance-point.component';
import { MaterialModule } from '../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FlexLayoutModule } from '@angular/flex-layout';
import { CanActiveUserGuard } from '../../_service/canActivate/can-active-user.guard';
import { PermissionGuard } from '../../_guard/permission.guard';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { SharedModule } from '../../_shared/shared.module';

const routes: Routes = [
  {
    path: '',
    component: CompanyComponent,
    children: [
      { path: 'new', component: UpdateCompanyComponent },
      { path: 'edit/:idCompany', component: UpdateCompanyComponent },
    ],
    canActivate: [CanActiveUserGuard, PermissionGuard],
    data: { permission: 'COMPANY_SETTINGS' },
  },
  {
    path: 'company-info',
    component: CompanyInfoComponent,
    canActivate: [CanActiveUserGuard],
    // Solo ver info - sin validación de feature
  },
];

/**
 * Módulo de Compañías (Lazy Loaded)
 *
 * Carga solo cuando se navega a /company
 * Incluye gestión de empresas, usuarios por empresa e información de empresa
 */
@NgModule({
  declarations: [
    CompanyComponent,
    UpdateCompanyComponent,
    CompanyInfoComponent,
    EstablishmentListComponent,
    UpdateEstablishmentComponent,
    IssuancePointListComponent,
    UpdateIssuancePointComponent,
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
export class CompanyModule {}
