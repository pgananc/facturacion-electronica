import {MaterialModule} from './material/material.module';
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LoginComponent} from './pages/login/login.component';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ServerErrorsInterceptor} from './_shared/server-errors.interceptor';
import {WelcomeComponent} from './pages/welcome/welcome.component';
import {ClientComponent} from './pages/customer/client/client.component';
import {UpdateClientComponent} from './pages/customer/client/update-client/update-client.component';
import {CompanyComponent} from './pages/company/company.component';
import {UpdateCompanyComponent} from './pages/company/update-company/update-company.component';
import {ProductComponent} from './pages/product/product.component';
import {UpdateProductComponent} from './pages/product/update-product/update-product.component';
import {UserComponent} from './pages/user/user.component';
import {UpdateUserComponent} from './pages/user/update-user/update-user.component';
import {Not403Component} from './pages/error/not403/not403.component';
import {Not404Component} from './pages/error/not404/not404.component';
import {ResetPasswordComponent} from './pages/login/reset-password/reset-password.component';
import {TokenComponent} from './pages/login/reset-password/token/token.component';
import {environment} from 'src/environments/environment';
import {JwtModule} from '@auth0/angular-jwt';
import {ProfileComponent} from './pages/profile/profile.component';
import {CompanyClientComponent} from './pages/company/company-client/company-client.component';
import {HashLocationStrategy, LocationStrategy} from '@angular/common';
import {CompanyUserComponent} from './pages/company/company-user/company-user.component';
import {CompanyDialogComponent} from './pages/welcome/company-dialog/company-dialog.component';
import {CompanyInfoComponent} from './pages/company/company-info/company-info.component';

export function tokenGetter() {
  let token = sessionStorage.getItem(environment.TOKEN);
  return token != null ? token : '';
}
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
    CompanyUserComponent,
    UserComponent,
    UpdateUserComponent,
    Not403Component,
    Not404Component,
    ResetPasswordComponent,
    TokenComponent,
    ProfileComponent,
    CompanyDialogComponent,
    CompanyInfoComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MaterialModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    JwtModule.forRoot({
      config: {
        tokenGetter: tokenGetter,
        allowedDomains: ['localhost:8180', 'localhost:8280', 'localhost:8380'],
        disallowedRoutes: [
          'http://TU_IP_PUBLICA o LOCALHOST/login/enviarCorreo',
        ],
      },
    }),
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ServerErrorsInterceptor,
      multi: true,
    },
    { provide: LocationStrategy, useClass: HashLocationStrategy },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
