import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder } from '@angular/forms';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AddPaymentDialogComponent } from './add-payment-dialog.component';
import { PaymentMethodService } from '../../../_service/invoice/payment-method.service';
import { LoggerService } from '../../../_service/logger/logger.service';

describe('AddPaymentDialogComponent', () => {
  let component: AddPaymentDialogComponent;
  let fixture: ComponentFixture<AddPaymentDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [AddPaymentDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: {} },
        { provide: MAT_DIALOG_DATA, useValue: {} },
        FormBuilder,
        PaymentMethodService,
        {
          provide: LoggerService,
          useValue: jasmine.createSpyObj('LoggerService', [
            'debug',
            'info',
            'warn',
            'error',
          ]),
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddPaymentDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
