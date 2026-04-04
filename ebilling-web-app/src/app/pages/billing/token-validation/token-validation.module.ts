import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

import { MaterialModule } from '../../../material/material.module';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TokenValidationComponent } from './token-validation.component';

const routes: Routes = [
  {
    path: '',
    component: TokenValidationComponent,
  },
];

@NgModule({
  declarations: [TokenValidationComponent],
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    MatProgressSpinnerModule,
    HttpClientModule,
  ],
})
export class TokenValidationModule {}
