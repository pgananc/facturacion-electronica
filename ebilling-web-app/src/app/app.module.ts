import { MaterialModule } from './material/material.module';
import { NgModule } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { LoginComponent } from './pages/login/login.component';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ServerErrorsInterceptor } from './_shared/server-errors.interceptor';
import { HttpTimeoutInterceptor } from './_shared/http-timeout.interceptor';
import { WelcomeComponent } from './pages/welcome/welcome.component';
import { Not403Component } from './pages/error/not403/not403.component';
import { Not404Component } from './pages/error/not404/not404.component';
import { ResetPasswordComponent } from './pages/login/reset-password/reset-password.component';
import { TokenComponent } from './pages/login/reset-password/token/token.component';
import { environment } from 'src/environments/environment';
import { JwtModule } from '@auth0/angular-jwt';
import { ProfileComponent } from './pages/profile/profile.component';
import { MatListModule } from '@angular/material/list';
import { MatDialogModule } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';

import { NgOptimizedImage } from '@angular/common';
import { CompanyDialogComponent } from './pages/welcome/company-dialog/company-dialog.component';
import { FlexLayoutModule } from '@angular/flex-layout';
import { SelectInvoicePointDialogComponent } from './pages/welcome/select-invoice-point-dialog/select-invoice-point-dialog.component';
import { MenuItemComponent } from './_shared/menu-item/menu-item.component';
import { SharedModule } from './_shared/shared.module';
import { SubscriptionBadgeComponent } from './subscription-badge/subscription-badge.component';

export function tokenGetter() {
  // Usar localStorage para persistir token entre cierres de browser
  // Esto permite restaurar la sesión si el usuario cierra accidentalmente
  let token = localStorage.getItem(environment.TOKEN);
  return token ?? '';
}

/**
 * Extrae el dominio de una URL completa
 * Ejemplo: 'http://localhost:8280' -> 'localhost:8280'
 *          'https://85.239.237.88:444' -> '85.239.237.88:444'
 */
function extractDomain(url: string): string {
  try {
    const urlObj = new URL(url);
    return urlObj.host; // host incluye hostname:port
  } catch (error) {
    console.error(`Error al extraer dominio de ${url}:`, error);
    return '';
  }
}

/**
 * Construye la lista de dominios permitidos desde environment
 */
function getAllowedDomains(): string[] {
  const domains = [
    environment.HOST_AUTH,
    environment.HOST_CUSTOMERS,
    environment.HOST_PRODUCTS,
    environment.HOST_EBILLING,
    environment.HOST_REPORT,
  ];
  const allowedDomains = domains.map(extractDomain).filter((d) => d !== '');
  if (!environment.production) {
    console.debug('[JwtModule] Dominios permitidos para JWT:', allowedDomains);
  }
  return allowedDomains;
}

/**
 * Construye las rutas públicas que NO deben incluir JWT
 */
function getPublicRoutes(): string[] {
  const routes = [
    `${environment.HOST_AUTH}/api/mail/send`,
    `${environment.HOST_AUTH}/api/token/restore-password/validate`,
    `${environment.HOST_AUTH}/api/login`,
  ];
  if (!environment.production) {
    console.debug('[JwtModule] Rutas públicas (sin JWT):', routes);
  }
  return routes;
}

/**
 * AppModule Principal
 *
 * Solo declara componentes que se cargan inmediatamente:
 * - Login, Welcome, Profile
 * - Páginas de error (403, 404)
 * - Reset password
 * - Diálogos compartidos
 *
 * Componentes como Billing, Products, Users, etc. ahora se cargan con Lazy Loading
 */
@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    WelcomeComponent,
    Not403Component,
    Not404Component,
    ResetPasswordComponent,
    TokenComponent,
    ProfileComponent,
    CompanyDialogComponent,
    SelectInvoicePointDialogComponent,
    MenuItemComponent,
    SubscriptionBadgeComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MaterialModule,
    SharedModule, // 🔧 Importar SharedModule para usar HasPermissionDirective
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCardModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDialogModule,
    MatListModule,
    MatExpansionModule,

    /**
     * Configuración JWT dinámica basada en environment
     *
     * - allowedDomains: Extrae automáticamente los dominios de HOST_AUTH, HOST_CUSTOMERS, etc.
     * - disallowedRoutes: Rutas públicas que NO requieren JWT (login, reset-password, mail)
     *
     * En DEV:  localhost:8180, localhost:8280, etc.
     * En PROD: 85.239.237.88:444 (único dominio para todos los microservicios)
     */
    JwtModule.forRoot({
      config: {
        tokenGetter: tokenGetter,
        allowedDomains: getAllowedDomains(),
        disallowedRoutes: getPublicRoutes(),
      },
    }),
    NgOptimizedImage,
  ],
  providers: [
    // Timeout interceptor: debe ejecutarse ANTES del ServerErrorsInterceptor
    // para que los errores de timeout se manejen correctamente
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HttpTimeoutInterceptor,
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ServerErrorsInterceptor,
      multi: true,
    },
    // PathLocationStrategy es el default de Angular
    // Ya no necesitamos especificar LocationStrategy
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
