import { Component, OnInit, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  PaymentMethodService,
  PaymentMethodDto,
} from '../../../_service/invoice/payment-method.service';
import { LoggerService } from '../../../_service/logger/logger.service';

@Component({
  selector: 'app-add-payment-dialog',
  templateUrl: './add-payment-dialog.component.html',
  styleUrls: ['./add-payment-dialog.component.css'],
})
export class AddPaymentDialogComponent implements OnInit {
  paymentForm: FormGroup;
  paymentMethods: PaymentMethodDto[] = [];
  timeUnits = ['Años', 'Meses', 'Días'];
  isLoading = true;

  constructor(
    private readonly dialogRef: MatDialogRef<AddPaymentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private readonly fb: FormBuilder,
    private readonly paymentMethodService: PaymentMethodService,
    private readonly logger: LoggerService
  ) {
    this.paymentForm = this.fb.group({
      paymentMethodId: [null, Validators.required],
      value: [0, [Validators.required, Validators.min(0)]],
      term: [null],
      timeUnit: [null],
    });
  }

  ngOnInit(): void {
    this.loadPaymentMethods();
  }

  loadPaymentMethods(): void {
    this.logger.debug('Cargando formas de pago desde el backend');

    this.paymentMethodService.findAll().subscribe({
      next: (methods) => {
        this.paymentMethods = methods;
        this.isLoading = false;
        this.logger.debug('Formas de pago cargadas', {
          total: methods.length,
        });
      },
      error: (err) => {
        this.logger.error('Error al cargar formas de pago', err);
        this.isLoading = false;
        // Opcional: mostrar un snackbar con el error
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onSave(): void {
    if (this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      return;
    }

    const selectedMethod = this.paymentMethods.find(
      (pm) => pm.idPaymentMethod === this.paymentForm.value.paymentMethodId
    );

    if (!selectedMethod) {
      this.logger.error('Método de pago no encontrado');
      return;
    }

    const paymentData = {
      idPaymentMethod: selectedMethod.idPaymentMethod,
      paymentType: selectedMethod.paymentMethodType,
      paymentMethodCode: selectedMethod.paymentMethodCode,
      value: this.paymentForm.value.value,
      term: this.paymentForm.value.term,
      time: this.paymentForm.value.timeUnit,
    };

    this.logger.debug('Forma de pago guardada', paymentData);
    this.dialogRef.close(paymentData);
  }

  getDisplayName(method: PaymentMethodDto): string {
    return `${method.paymentMethodCode} - ${method.paymentMethodType}`;
  }
}
