import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { WelcomeComponent } from './pages/welcome/welcome.component';
import { ClientComponent } from './pages/customer/client/client.component';
import { UpdateClientComponent } from './pages/customer/client/update-client/update-client.component';
import { CompanyComponent } from './pages/company/company.component';
import { UpdateCompanyComponent } from './pages/company/update-company/update-company.component';
import { ProductComponent } from './pages/product/product.component';
import { UpdateProductComponent } from './pages/product/update-product/update-product.component';
import { UserComponent } from './pages/user/user.component';
import { UpdateUserComponent } from './pages/user/update-user/update-user.component';
import { Not403Component } from './pages/error/not403/not403.component';
import { CanActiveUserGuard } from './_service/canActivate/can-active-user.guard';
import { Not404Component } from './pages/error/not404/not404.component';
import { ResetPasswordComponent } from './pages/login/reset-password/reset-password.component';
import { TokenComponent } from './pages/login/reset-password/token/token.component';
import { CompanyClientComponent } from './pages/company/company-client/company-client.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { CompanyUserComponent } from './pages/company/company-user/company-user.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'profile', component: ProfileComponent },
  { path: 'not-403', component: Not403Component },

  {
    path: 'reset-password',
    component: ResetPasswordComponent,
    children: [{ path: ':token', component: TokenComponent }],
  },
  {
    path: 'welcome',
    component: WelcomeComponent,
  },
  {
    path: 'client',
    component: ClientComponent,
    children: [
      { path: 'new', component: UpdateClientComponent },
      { path: 'edit/:idClient', component: UpdateClientComponent },
    ],
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'product',
    component: ProductComponent,
    children: [
      { path: 'new', component: UpdateProductComponent },
      { path: 'edit/:idProduct', component: UpdateProductComponent },
    ],
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'user',
    component: UserComponent,
    children: [
      { path: 'new', component: UpdateUserComponent },
      { path: 'edit/:idUser', component: UpdateUserComponent },
    ],
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'company',
    component: CompanyComponent,
    children: [
      { path: 'new', component: UpdateCompanyComponent },
      { path: 'edit/:idCompany', component: UpdateCompanyComponent },
    ],
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'company-client',
    component: CompanyClientComponent,
    canActivate: [CanActiveUserGuard],
  },
  {
    path: 'company-user',
    component: CompanyUserComponent,
    canActivate: [CanActiveUserGuard],
  },

  { path: '**', component: Not404Component },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
