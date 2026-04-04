import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../../material/material.module';
import { AdminIssuancePointListComponent } from './admin-issuance-point-list.component';

const routes: Routes = [
  {
    path: '',
    component: AdminIssuancePointListComponent,
  },
];

@NgModule({
  declarations: [AdminIssuancePointListComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MaterialModule,
    RouterModule.forChild(routes),
  ],
})
export class AdminIssuancePointListModule {}
