import {MaterialModule} from './material/material.module';
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LoginComponent} from './pages/login/login.component';
import {HttpClientModule, HTTP_INTERCEPTORS} from '@angular/common/http';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ServerErrorsInterceptor} from './_shared/server-errors.interceptor';
import {WelcomeComponent} from './pages/welcome/welcome.component';
import {ClientComponent} from './pages/customer/client/client.component';
import {UpdateClientComponent} from './pages/customer/client/update-client/update-client.component';
import {CompanyComponent} from "./pages/company/company.component";
import {UpdateCompanyComponent} from "./pages/company/update-company/update-company.component";
import {CompanyClientComponent} from "./pages/company/company-client/company-client.component";
import {ProductComponent} from './pages/product/product.component';
import {UpdateProductComponent} from './pages/product/update-product/update-product.component';
import {UserComponent} from './pages/user/user.component';
import {UpdateUserComponent} from './pages/user/update-user/update-user.component';
import {Not403Component} from './pages/error/not403/not403.component';
import {Not404Component} from './pages/error/not404/not404.component';
import {ResetPasswordComponent} from './pages/login/reset-password/reset-password.component';
import {TokenComponent} from './pages/login/reset-password/token/token.component';
import {HashLocationStrategy, LocationStrategy} from "@angular/common";

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    WelcomeComponent,
    ClientComponent,
    UpdateClientComponent,
    ProductComponent,
    UpdateProductComponent,
    CompanyClientComponent,
    CompanyComponent,
    UpdateCompanyComponent,
    UserComponent,
    UpdateUserComponent,
    Not403Component,
    Not404Component,
    ResetPasswordComponent,
    TokenComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MaterialModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ServerErrorsInterceptor,
      multi: true,
    },
    {provide: LocationStrategy, useClass: HashLocationStrategy},
  ],
  bootstrap: [AppComponent],
})
export class AppModule {
}
