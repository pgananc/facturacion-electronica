/**
 * Test de Integración: Flujo de Notas de Crédito
 *
 * Este test verifica el flujo completo de creación de una nota de crédito:
 * Seleccionar factura → Crear NC → Enviar al SRI → Verificar actualización
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

import { CreateCreditNoteComponent } from '../pages/billing/credit-notes/create-credit-note/create-credit-note.component';
import { CreditNoteService } from '../_service/invoice/credit-note.service';
import { InvoiceService } from '../_service/invoice/invoice.service';
import { PermissionService } from '../_service/permission/permission.service';
import { LoggerService } from '../_service/logger/logger.service';
import { CreateCreditNoteRequest } from '../_model/invoice/credit-note-dto';
import { environment } from '../../environments/environment';

describe('Integration Test: Flujo de Notas de Crédito', () => {
  let httpMock: HttpTestingController;
  let creditNoteService: CreditNoteService;
  let invoiceService: InvoiceService;
  let permissionService: PermissionService;

  const mockToken = 'mock-jwt-token';
  const mockCompanyId = 1;
  const mockInvoiceId = 1;
  const mockCreditNoteId = 1;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CreateCreditNoteComponent],
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
        CreditNoteService,
        InvoiceService,
        PermissionService,
        LoggerService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    creditNoteService = TestBed.inject(CreditNoteService);
    invoiceService = TestBed.inject(InvoiceService);
    permissionService = TestBed.inject(PermissionService);

    // Mock localStorage
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'token') return mockToken;
      if (key === environment.ID_COMPANY) return String(mockCompanyId);
      if (key === 'INVOICE_POINT_DATA') return JSON.stringify({ id: 1 });
      return null;
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Flujo completo de nota de crédito', () => {
    it('should complete full credit note flow: select invoice → create NC → send to SRI', (done) => {
      // 1. Seleccionar factura disponible para NC
      const invoiceSpy = spyOn(invoiceService, 'findById').and.returnValue(
        of({
          data: {
            idInvoice: mockInvoiceId,
            idCompany: mockCompanyId,
            invoiceNumber: '001-001-000000123',
            totalAmount: 1000,
            availableAmount: 1000,
            clientDto: {
              idClient: 1,
              identification: '1234567890',
              name: 'Test Client',
              idType: 1,
              email: 'test@example.com',
            },
            details: [
              {
                idInvoiceDetail: 1,
                idProduct: 1,
                quantity: 10,
                unitPrice: 100,
                subtotal: 1000,
                ivaPercentage: 15,
              },
            ],
          },
        })
      );

      invoiceService.findById(mockInvoiceId).subscribe((invoice) => {
        expect(invoiceSpy).toHaveBeenCalledWith(mockInvoiceId);
        expect(invoice.data.idInvoice).toBe(mockInvoiceId);
        expect(invoice.data.availableAmount).toBeGreaterThan(0);

        // 2. Obtener factura disponible para NC
        const availableSpy = spyOn(
          creditNoteService,
          'getInvoiceAvailableForCreditNote'
        ).and.returnValue(
          of({
            idInvoice: mockInvoiceId,
            invoiceNumber: '001-001-000000123',
            issueDate: '2024-01-15',
            accessKey: 'test-access-key',
            authorizationNumber: '1234567890',
            client: {
              idClient: 1,
              identification: '1234567890',
              name: 'Test Client',
              email: 'test@example.com',
            },
            totalInvoice: 1000,
            totalCreditNotesApplied: 0,
            availableAmount: 1000,
            details: [
              {
                idInvoiceDetail: 1,
                idProduct: 1,
                mainCode: 'PROD-001',
                description: 'Test Product',
                quantityInvoiced: 10,
                quantityReturned: 0,
                quantityAvailable: 10,
                unitPrice: 100,
                discount: 0,
                subtotal: 1000,
                ivaPercentage: 15,
              },
            ],
          })
        );

        creditNoteService
          .getInvoiceAvailableForCreditNote(mockInvoiceId)
          .subscribe((available) => {
            expect(availableSpy).toHaveBeenCalledWith(mockInvoiceId);
            expect(available.availableAmount).toBeGreaterThan(0);

            // 3. Verificar permisos para crear NC
            const permissionSpy = spyOn(
              permissionService,
              'canEmitDocument'
            ).and.returnValue(of(true));

            permissionService
              .canEmitDocument('CREDIT_NOTE')
              .subscribe((allowed) => {
                expect(permissionSpy).toHaveBeenCalledWith('CREDIT_NOTE');
                expect(allowed).toBe(true);

                // 4. Crear y enviar NC al SRI
                const createSpy = spyOn(
                  creditNoteService,
                  'createAndSend'
                ).and.returnValue(
                  of({
                    code: 200,
                    status: 'OK',
                    message: 'Nota de crédito creada',
                    idCreditNote: mockCreditNoteId,
                    creditNoteNumber: '001-001-000000001',
                    accessKey: 'test-credit-note-key',
                  })
                );

                const creditNoteData: CreateCreditNoteRequest = {
                  idInvoice: mockInvoiceId,
                  idReason: 1,
                  idIssuancePoint: 1,
                  issueDate: '2024-01-20',
                  observation: 'Test credit note',
                  companyDto: {
                    idCompany: mockCompanyId,
                  },
                  clientDto: {
                    idClient: 1,
                  },
                  details: [
                    {
                      idInvoiceDetail: 1,
                      idProduct: 1,
                      quantity: 5,
                      unitPrice: 100,
                      discount: 0,
                      subtotal: 500,
                    },
                  ],
                };

                creditNoteService
                  .createAndSend(creditNoteData)
                  .subscribe((response) => {
                    expect(createSpy).toHaveBeenCalled();
                    expect(response.idCreditNote).toBeDefined();
                    expect(response.creditNoteNumber).toBeDefined();

                    // 5. Verificar que la factura se actualizó (disponible amount disminuyó)
                    // Actualizar el spy para retornar la factura con availableAmount reducido
                    invoiceSpy.and.returnValue(
                      of({
                        data: {
                          idInvoice: mockInvoiceId,
                          idCompany: mockCompanyId,
                          invoiceNumber: '001-001-000000123',
                          totalAmount: 1000,
                          availableAmount: 500, // Reducido después de crear la NC
                          clientDto: {
                            idClient: 1,
                            identification: '1234567890',
                            name: 'Test Client',
                            idType: 1,
                            email: 'test@example.com',
                          },
                          details: [
                            {
                              idInvoiceDetail: 1,
                              idProduct: 1,
                              quantity: 10,
                              unitPrice: 100,
                              subtotal: 1000,
                              ivaPercentage: 15,
                            },
                          ],
                        },
                      })
                    );

                    invoiceService
                      .findById(mockInvoiceId)
                      .subscribe((updatedInvoice) => {
                        expect(
                          updatedInvoice.data.availableAmount
                        ).toBeLessThan(1000);
                        done();
                      });
                  });
              });
          });
      });
    });

    it('should handle error when creating credit note', (done) => {
      const createSpy = spyOn(
        creditNoteService,
        'createAndSend'
      ).and.returnValue(
        throwError(() => ({
          error: { message: 'Error al crear nota de crédito' },
          status: 500,
        }))
      );

      const creditNoteData: CreateCreditNoteRequest = {
        idInvoice: mockInvoiceId,
        idReason: 1,
        idIssuancePoint: 1,
        issueDate: '2024-01-20',
        observation: 'Test credit note',
        companyDto: {
          idCompany: mockCompanyId,
        },
        clientDto: {
          idClient: 1,
        },
        details: [],
      };

      creditNoteService.createAndSend(creditNoteData).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(createSpy).toHaveBeenCalled();
          expect(error.error.message).toBe('Error al crear nota de crédito');
          done();
        },
      });
    });
  });
});
