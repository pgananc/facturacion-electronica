import { Component, OnInit, Inject, OnDestroy } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { CarrierDto } from '../../../_model/guide/carrier-dto';
import { CarrierService } from '../../../_service/guide/carrier.service';
import { catchError, takeUntil } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { CARRIER_IDENTIFICATION_TYPE } from '../../../_constants/constants';
import { ToastService } from '../../../_service/notification/toast.service';
import { environment } from 'src/environments/environment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { SUCCESS, UPDATE } from '../../../_constants/constants';

export interface CarrierDialogData {
  idCarrier?: number;
  mode: 'create' | 'edit';
  initialIdentification?: string; // Identificación inicial para pre-llenar (desde guía)
  initialIdType?: string; // Tipo de identificación inicial para pre-llenar (desde guía)
}

@Component({
  selector: 'app-update-carrier',
  templateUrl: './update-carrier.component.html',
  styleUrls: ['./update-carrier.component.css'],
})
export class UpdateCarrierComponent implements OnInit, OnDestroy {
  idCarrier: number = 0;
  carrier: CarrierDto = {} as CarrierDto;

  form: FormGroup;
  edition: boolean = false;
  idTypes = CARRIER_IDENTIFICATION_TYPE;

  private destroy$ = new Subject<void>();

  constructor(
    private readonly carrierService: CarrierService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    public dialogRef: MatDialogRef<UpdateCarrierComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CarrierDialogData
  ) {}

  ngOnInit(): void {
    this.carrier = {} as CarrierDto;

    this.form = new FormGroup({
      id: new FormControl(0),
      idCompany: new FormControl(
        Number(localStorage.getItem(environment.ID_COMPANY))
      ),
      identificationType: new FormControl('', Validators.required),
      identification: new FormControl('', Validators.required),
      name: new FormControl('', Validators.required),
      address: new FormControl(''),
      phone: new FormControl(''),
      email: new FormControl('', [Validators.email]),
      licenseNumber: new FormControl(''),
      vehicleType: new FormControl(''),
      status: new FormControl(true),
    });

    // ✅ Subscribe to identification type changes to update validations dynamically
    this.form
      .get('identificationType')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((idType: string) => {
        this.updateIdentificationValidation(idType);
      });

    // Initialize based on dialog data
    if (this.data.mode === 'edit' && this.data.idCarrier) {
      this.idCarrier = this.data.idCarrier;
      this.edition = true;
      this.loadCarrier();
    } else {
      this.edition = false;
      // ✅ Pre-llenar campos si vienen desde guía de remisión
      const initialIdType = this.data.initialIdType || '';
      const initialIdentification = this.data.initialIdentification || '';

      // ✅ Primero establecer el tipo de identificación y sus validaciones
      if (initialIdType) {
        this.updateIdentificationValidation(initialIdType);
      }

      // Luego pre-llenar valores
      if (initialIdType) {
        this.form.patchValue({ identificationType: initialIdType });
      }

      // Pre-llenar identificación si viene desde guía (después de actualizar validaciones)
      if (initialIdentification) {
        this.form.patchValue({ identification: initialIdentification });
      }

      // ✅ Actualizar el estado de validación del formulario después de pre-llenar
      setTimeout(() => {
        this.form
          .get('identificationType')
          ?.updateValueAndValidity({ emitEvent: true });
        this.form
          .get('identification')
          ?.updateValueAndValidity({ emitEvent: true });
        this.form.updateValueAndValidity({ emitEvent: true });
      }, 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCarrier(): void {
    this.carrierService
      .getCarrierById(this.idCarrier)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error: HttpErrorResponse) => {
          this.logger.error('Error loading carrier', error);
          this.toast.error('Error al cargar transportista');
          this.dialogRef.close();
          return [];
        })
      )
      .subscribe((carrier: CarrierDto) => {
        this.carrier = carrier;
        this.form.patchValue({
          id: carrier.idCarrier,
          idCompany: carrier.idCompany,
          identificationType: carrier.identificationType,
          identification: carrier.identification,
          name: carrier.name,
          address: carrier.address || '',
          phone: carrier.phone || '',
          email: carrier.email || '',
          licenseNumber: carrier.licenseNumber || '',
          vehicleType: carrier.vehicleType || '',
          status: carrier.status !== undefined ? carrier.status : true,
        });

        // ✅ Aplicar validaciones según el tipo de identificación cargado
        if (carrier.identificationType) {
          this.updateIdentificationValidation(carrier.identificationType);
        }
      });
  }

  validateSave(): void {
    if (this.form.valid) {
      this.save();
    } else {
      this.form.markAllAsTouched();
      this.toast.warning(
        'Por favor complete todos los campos requeridos'
      );
    }
  }

  save(): void {
    const carrierDto: CarrierDto = {
      idCarrier: this.form.get('id')?.value || undefined,
      idCompany: this.form.get('idCompany')?.value,
      identificationType: this.form.get('identificationType')?.value,
      identification: this.form.get('identification')?.value,
      name: this.form.get('name')?.value,
      address: this.form.get('address')?.value || undefined,
      phone: this.form.get('phone')?.value || undefined,
      email: this.form.get('email')?.value || undefined,
      licenseNumber: this.form.get('licenseNumber')?.value || undefined,
      vehicleType: this.form.get('vehicleType')?.value || undefined,
      status: this.form.get('status')?.value !== false,
    };

    if (this.edition) {
      this.updateCarrier(carrierDto);
    } else {
      this.createCarrier(carrierDto);
    }
  }

  createCarrier(carrier: CarrierDto): void {
    this.carrierService
      .createCarrier(carrier)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error: HttpErrorResponse) => {
          this.logger.error('Error creating carrier', error);
          const errorMessage =
            error.error?.message ||
            'Error al crear transportista. Por favor intente nuevamente.';

          this.toast.error(errorMessage);

          return [];
        })
      )
      .subscribe((response: CarrierDto) => {
        if (response) {
          this.toast.success(
            SUCCESS.MESSAGE_REGISTER_CLIENT.message.replace(
              'Cliente',
              'Transportista'
            )
          );
          // Retornar el carrier creado para que se pueda cargar en el formulario
          this.dialogRef.close(response);
        }
      });
  }

  updateCarrier(carrier: CarrierDto): void {
    if (!this.idCarrier) {
      this.toast.error('ID de transportista inválido');
      return;
    }

    this.carrierService
      .updateCarrier(this.idCarrier, carrier)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error: HttpErrorResponse) => {
          this.logger.error('Error updating carrier', error);
          const errorMessage =
            error.error?.message ||
            'Error al actualizar transportista. Por favor intente nuevamente.';
          this.toast.error(errorMessage);
          return [];
        })
      )
      .subscribe((response: CarrierDto) => {
        if (response) {
          this.toast.success(
            UPDATE.MESSAGE_UPDATE_CLIENT.message.replace(
              'Cliente',
              'Transportista'
            )
          );
          this.dialogRef.close(true);
        }
      });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  get f() {
    return this.form.controls;
  }

  /**
   * Retorna el placeholder apropiado según el tipo de identificación
   */
  getIdentificationPlaceholder(): string {
    const idType = this.form.get('identificationType')?.value;
    switch (idType) {
      case '1': // Cédula
        return 'Ej: 1712345678 (10 dígitos)';
      case '2': // RUC
        return 'Ej: 1712345678001 (13 dígitos)';
      case '3': // Pasaporte
        return 'Ej: AB123456 (alfanumérico, máx 10 caracteres)';
      default:
        return 'Número de identificación';
    }
  }

  /**
   * Actualiza las validaciones del campo identificación según el tipo seleccionado
   * Carrier usa códigos string: '1'=Cédula, '2'=RUC, '3'=Pasaporte
   */
  updateIdentificationValidation(idType: string): void {
    const identificationControl = this.form.get('identification');
    if (!identificationControl) return;

    // Limpiar validaciones previas
    identificationControl.clearValidators();

    switch (idType) {
      case '1': // Cédula: 10 dígitos numéricos
        identificationControl.setValidators([
          Validators.required,
          Validators.minLength(10),
          Validators.maxLength(10),
          Validators.pattern('^[0-9]{10}$'), // Solo números, exactamente 10
        ]);
        identificationControl.enable();
        break;

      case '2': // RUC: 13 dígitos numéricos
        identificationControl.setValidators([
          Validators.required,
          Validators.minLength(13),
          Validators.maxLength(13),
          Validators.pattern('^[0-9]{13}$'), // Solo números, exactamente 13
        ]);
        identificationControl.enable();
        break;

      case '3': // Pasaporte: Alfanumérico, máximo 10 caracteres
        identificationControl.setValidators([
          Validators.required,
          Validators.maxLength(10),
          Validators.pattern('^[A-Za-z0-9]{1,10}$'), // Solo letras y números, máx 10
        ]);
        identificationControl.enable();
        break;

      default:
        identificationControl.setValidators([Validators.required]);
        identificationControl.enable();
        break;
    }

    // Actualizar validaciones y resetear errores
    identificationControl.updateValueAndValidity();
  }
}
