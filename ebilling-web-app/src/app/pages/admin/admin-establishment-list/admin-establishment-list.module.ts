import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../../material/material.module';
import { AdminEstablishmentListComponent } from './admin-establishment-list.component';

const routes: Routes = [
  {
    path: '',
    component: AdminEstablishmentListComponent,
  },
];

@NgModule({
  declarations: [AdminEstablishmentListComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MaterialModule,
    RouterModule.forChild(routes),
  ],
})
export class AdminEstablishmentListModule {}
