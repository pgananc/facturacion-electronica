import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { MenuService } from './_service/auth/menu.service';
import { CompanyService } from './_service/customer/company.service';
import { LoginService } from './_service/auth/login.service';
import { PaymentMethodService } from './_service/invoice/payment-method.service';
import { LoggerService } from './_service/logger/logger.service';
import { MenuFilterService } from './_service/menu/menu-filter.service';
import { MatDialog } from '@angular/material/dialog';
import { JwtHelperService } from '@auth0/angular-jwt';
import { BreakpointObserver } from '@angular/cdk/layout';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [AppComponent],
      providers: [
        MenuService,
        CompanyService,
        LoginService,
        PaymentMethodService,
        LoggerService,
        MenuFilterService,
        { provide: MatDialog, useValue: {} },
        { provide: JwtHelperService, useValue: {} },
        {
          provide: BreakpointObserver,
          useValue: {
            observe: jasmine.createSpy('observe').and.returnValue({
              pipe: jasmine.createSpy('pipe').and.returnValue({
                subscribe: jasmine.createSpy('subscribe'),
              }),
            }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have as title 'ebilling-web-app'`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('ebilling-web-app');
  });

  it('should render router outlet', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    // El componente renderiza router-outlet, no un título específico
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });
});
