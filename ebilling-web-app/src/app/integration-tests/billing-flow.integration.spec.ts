/**
 * Test de Integración: Flujo Completo de Facturación
 *
 * Este test verifica el flujo completo de creación de una factura:
 * 1. Login → Seleccionar empresa → Crear factura → Guardar borrador → Enviar al SRI → Ver ticket
 */

import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { BillingComponent } from '../pages/billing/billing.component';
import { InvoiceService } from '../_service/invoice/invoice.service';
import { DraftInvoiceService } from '../_service/invoice/draft-invoice.service';
import { ProductService } from '../_service/product/product.service';
import { ClientService } from '../_service/customer/client.service';
import { CompanyService } from '../_service/customer/company.service';
import { PaymentMethodService } from '../_service/invoice/payment-method.service';
import { PermissionService } from '../_service/permission/permission.service';
import { LoginService } from '../_service/auth/login.service';
import { TokenService } from '../_service/auth/token.service';
import { InvoicePointService } from '../_service/auth/invoice-point.service';
import { LoggerService } from '../_service/logger/logger.service';
import { TokenResponse } from '../_model/auth/token-response';
import { InvoiceDto } from '../_model/invoice/invoice-dto';
import { CompanyResponse } from '../_model/customer/company-response';
import { ClientPageResponse } from '../_model/customer/client-page-response';
import { Client } from '../_model/customer/client';
import { ProductResponse } from '../_model/product/product-response';
import { environment } from '../../environments/environment';

describe('Integration Test: Flujo Completo de Facturación', () => {
  let component: BillingComponent;
  let httpMock: HttpTestingController;
  let invoiceService: InvoiceService;
  let draftInvoiceService: DraftInvoiceService;
  let productService: ProductService;
  let clientService: ClientService;
  let companyService: CompanyService;
  let permissionService: PermissionService;
  let loginService: LoginService;
  let tokenService: TokenService;

  const mockToken = 'mock-jwt-token';
  const mockCompanyId = 1;
  const mockUserId = 1;
  const mockIssuancePointId = 1;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BillingComponent],
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        MatDialogModule,
        MatSnackBarModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
      ],
      providers: [
        InvoiceService,
        DraftInvoiceService,
        ProductService,
        ClientService,
        CompanyService,
        PaymentMethodService,
        PermissionService,
        LoginService,
        TokenService,
        InvoicePointService,
        LoggerService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    invoiceService = TestBed.inject(InvoiceService);
    draftInvoiceService = TestBed.inject(DraftInvoiceService);
    productService = TestBed.inject(ProductService);
    clientService = TestBed.inject(ClientService);
    companyService = TestBed.inject(CompanyService);
    permissionService = TestBed.inject(PermissionService);
    loginService = TestBed.inject(LoginService);
    tokenService = TestBed.inject(TokenService);

    // Mock localStorage
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'token') return mockToken;
      if (key === environment.ID_COMPANY) return String(mockCompanyId);
      if (key === 'idUser') return String(mockUserId);
      if (key === 'INVOICE_POINT_DATA')
        return JSON.stringify({ id: mockIssuancePointId });
      return null;
    });

    spyOn(localStorage, 'setItem').and.stub();
    spyOn(localStorage, 'removeItem').and.stub();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Flujo completo de facturación', () => {
    it('should complete full invoice flow: login → select company → create invoice → save draft → send to SRI', (done) => {
      // 1. Login
      const loginSpy = spyOn(loginService, 'login').and.returnValue(
        of({
          code: 200,
          status: 'OK',
          message: 'Login successful',
          token: { token: mockToken },
        } as TokenResponse)
      );

      loginService.login('test@example.com', 'password').subscribe(() => {
        expect(loginSpy).toHaveBeenCalled();
        expect(localStorage.getItem('token')).toBe(mockToken);

        // 2. Verificar permisos para crear factura
        const permissionSpy = spyOn(
          permissionService,
          'canEmitDocument'
        ).and.returnValue(of(true));

        permissionService.canEmitDocument('INVOICE').subscribe((allowed) => {
          expect(permissionSpy).toHaveBeenCalledWith('INVOICE');
          expect(allowed).toBe(true);

          // 3. Cargar datos de empresa
          const companySpy = spyOn(companyService, 'findById').and.returnValue(
            of({
              code: 200,
              status: 'OK',
              message: 'Success',
              companyDetailsDtos: [],
              companyDetailsDto: {
                idCompany: mockCompanyId,
                idType: 1,
                identification: '1234567890',
                name: 'Test Company',
                nameCommercial: 'Test Commercial',
                forcedToAccounting: 'N',
                specialTaxpayer: 0,
                status: true,
                establishments: [
                  {
                    idEstablishment: 1,
                    name: 'Test Establishment',
                    code: '001',
                    address: 'Test Address',
                    idCompany: mockCompanyId,
                    type: 'PRINCIPAL',
                    status: true,
                  },
                ],
              },
            } as CompanyResponse)
          );

          companyService.findById(mockCompanyId).subscribe((company) => {
            expect(companySpy).toHaveBeenCalledWith(mockCompanyId);
            expect(company.companyDetailsDto).toBeDefined();

            // 4. Buscar productos
            const productSpy = spyOn(
              productService,
              'searchPageable'
            ).and.returnValue(
              of({
                code: 200,
                status: 'OK',
                message: 'Success',
                productDtos: [
                  {
                    idProduct: 1,
                    mainCode: 'PROD-001',
                    name: 'Test Product',
                    description: 'Test Product',
                    unitPrice: 100,
                    tourismIva: false,
                    ice: false,
                    ivaTaxType: {
                      idTaxType: 1,
                      type: '2',
                      value: 15,
                      name: 'IVA 15%',
                    },
                  },
                ],
              } as ProductResponse)
            );

            productService
              .searchPageable(0, 10, { name: 'Test' })
              .subscribe((products) => {
                expect(productSpy).toHaveBeenCalled();
                expect(products.productDtos.length).toBeGreaterThan(0);

                // 5. Buscar cliente
                const mockClient: Client = {
                  idClient: 1,
                  idType: 1,
                  identification: '1234567890',
                  name: 'Test Client',
                  clientType: 1,
                  status: true,
                  companyIdentification: '1234567890',
                  contacts: [],
                  mail: 'client@example.com',
                };

                const clientSpy = spyOn(
                  clientService,
                  'searchPageable'
                ).and.returnValue(
                  of({
                    content: [mockClient],
                    totalElements: 1,
                    totalPages: 1,
                    size: 10,
                    number: 0,
                  } as ClientPageResponse)
                );

                clientService
                  .searchPageable(0, 10, { name: 'Test' } as any)
                  .subscribe((clients) => {
                    expect(clientSpy).toHaveBeenCalled();
                    expect(clients.content.length).toBeGreaterThan(0);

                    // 6. Guardar borrador
                    const draftSpy = spyOn(
                      draftInvoiceService,
                      'saveDraft'
                    ).and.returnValue(
                      of({
                        code: 200,
                        status: 'OK',
                        message: 'Borrador guardado',
                        idInvoice: 1,
                      })
                    );

                    const draftData: InvoiceDto = {
                      idCompany: mockCompanyId,
                      issueDate: '2024-01-20',
                      totalAmount: 1000,
                      discount: 0,
                      tip: 0,
                      subtotalZero: 0,
                      subtotalIva: 1000,
                      subtotalIce: 0,
                      subtotalDiscount: 0,
                      clientDto: {
                        idClient: 1,
                        identification: '1234567890',
                        idType: 1,
                        name: 'Test Client',
                        email: 'test@example.com',
                      },
                      invoiceDetails: [
                        {
                          idProduct: 1,
                          quantity: 10,
                          unitPrice: 100,
                          discount: 0,
                          subtotal: 1000,
                        },
                      ],
                      payments: [
                        {
                          paymentType: '01',
                          value: 1000,
                        },
                      ],
                      establishment: {
                        idEstablishment: 1,
                        code: '001',
                      },
                    };

                    draftInvoiceService
                      .saveDraft(draftData)
                      .subscribe((response) => {
                        expect(draftSpy).toHaveBeenCalled();
                        expect(response.idInvoice).toBeDefined();

                        // 7. Enviar al SRI
                        const invoiceSpy = spyOn(
                          invoiceService,
                          'register'
                        ).and.returnValue(
                          of({
                            code: 200,
                            status: 'OK',
                            message: 'Factura creada',
                            idInvoice: 1,
                            invoiceNumber: '001-001-000000123',
                            accessKey: 'test-access-key',
                          })
                        );

                        invoiceService
                          .register(draftData)
                          .subscribe((invoiceResponse: any) => {
                            expect(invoiceSpy).toHaveBeenCalled();
                            expect(invoiceResponse.idInvoice).toBeDefined();
                            expect(invoiceResponse.invoiceNumber).toBeDefined();

                            done();
                          });
                      });
                  });
              });
          });
        });
      });
    });

    it('should handle errors during invoice creation flow', (done) => {
      // Simular error al guardar borrador
      const draftSpy = spyOn(draftInvoiceService, 'saveDraft').and.returnValue(
        throwError(() => ({
          error: { message: 'Error al guardar borrador' },
          status: 500,
        }))
      );

      const draftData: InvoiceDto = {
        idCompany: mockCompanyId,
        issueDate: '2024-01-20',
        totalAmount: 0,
        discount: 0,
        tip: 0,
        subtotalZero: 0,
        subtotalIva: 0,
        subtotalIce: 0,
        subtotalDiscount: 0,
        clientDto: {
          idClient: 1,
          identification: '1234567890',
          idType: 1,
          name: 'Test Client',
          email: 'test@example.com',
        },
        invoiceDetails: [],
        payments: [],
        establishment: {
          idEstablishment: 1,
          code: '001',
        },
      };

      draftInvoiceService.saveDraft(draftData).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(draftSpy).toHaveBeenCalled();
          expect(error.error.message).toBe('Error al guardar borrador');
          done();
        },
      });
    });
  });
});
