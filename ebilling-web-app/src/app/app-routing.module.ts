import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { WelcomeComponent } from './pages/welcome/welcome.component';
import { ClientComponent } from './pages/customer/client/client.component';
import { UpdateClientComponent } from './pages/customer/client/update-client/update-client.component';
import {ProductComponent} from "./pages/product/product.component";
import {UpdateProductComponent} from "./pages/product/update-product/update-product.component";
import {CompanyComponent} from "./pages/company/company.component";
import {UpdateCompanyComponent} from "./pages/company/update-company/update-company.component";

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'welcome', component: WelcomeComponent },
  {
    path: 'client',
    component: ClientComponent,
    children: [
      { path: 'new', component: UpdateClientComponent },
      { path: 'edit/:idClient', component: UpdateClientComponent },
    ],
  },
  {
    path: 'product',
    component: ProductComponent,
    children: [
      { path: 'new', component: UpdateProductComponent },
      { path: 'edit/:idProduct', component: UpdateProductComponent },
    ]
  },
  {
    path: 'company',
    component: CompanyComponent,
    children: [
      { path: 'new', component: UpdateCompanyComponent },
      { path: 'edit/:idCompany', component: UpdateCompanyComponent },
    ]
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
