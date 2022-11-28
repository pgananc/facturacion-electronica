import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { WelcomeComponent } from './pages/welcome/welcome.component';
import { ClientComponent } from './pages/customer/client/client.component';
import { UpdateClientComponent } from './pages/customer/client/update-client/update-client.component';
import { ProductComponent } from './pages/product/product.component';
import { UpdateProductComponent } from './pages/product/update-product/update-product.component';
import { UserComponent } from './pages/user/user.component';
import { UpdateUserComponent } from './pages/user/update-user/update-user.component';
import { Not403Component } from './pages/error/not403/not403.component';
import { CanActiveUserGuard } from './_service/canActivate/can-active-user.guard';
import { Not404Component } from './pages/error/not404/not404.component';
import { ResetPasswordComponent } from './pages/login/reset-password/reset-password.component';
import { TokenComponent } from './pages/login/reset-password/token/token.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
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
  { path: '**', component: Not404Component },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
