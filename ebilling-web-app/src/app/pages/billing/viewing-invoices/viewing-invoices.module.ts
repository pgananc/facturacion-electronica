import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { ViewingInvoicesComponent } from './viewing-invoices.component';
import { MaterialModule } from '../../../material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { CanActiveUserGuard } from '../../../_service/canActivate/can-active-user.guard';
import { ClientTokenGuard } from 'src/app/_service/canActivate/client-token.guard';

const routes: Routes = [
  {
    path: '',
    component: ViewingInvoicesComponent,
    canActivate: [ClientTokenGuard],
  },
];

@NgModule({
  declarations: [ViewingInvoicesComponent],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCheckboxModule,
  ],
})
export class ViewingInvoicesModule {}
