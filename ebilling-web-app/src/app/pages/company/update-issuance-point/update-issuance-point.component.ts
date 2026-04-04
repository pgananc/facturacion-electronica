import {
  Component,
  OnInit,
  Inject,
  ViewEncapsulation,
  AfterViewInit,
  ElementRef,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { InvoiceIssuancePoint } from '../../../_model/customer/invoice-issuance-point';
import {
  IssuancePointService,
  IssuancePointRequest,
} from '../../../_service/customer/issuance-point.service';
import { EstablishmentService } from '../../../_service/customer/establishment.service';
import { CompanyService } from '../../../_service/customer/company.service';
import {
  Establishment,
  Company,
} from '../../../_model/customer/company-establishment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';

export interface IssuancePointDialogData {
  point?: InvoiceIssuancePoint;
  idCompany?: number; // ✅ Ahora es opcional
  isSuperAdmin?: boolean; // ✅ OPCIÓN A: Flag para super admin
}

@Component({
  selector: 'app-update-issuance-point',
  templateUrl: './update-issuance-point.component.html',
  styleUrls: ['./update-issuance-point.component.css'],
  encapsulation: ViewEncapsulation.None, // ✅ CRÍTICO: Permitir que CSS override Material
})
export class UpdateIssuancePointComponent implements OnInit, AfterViewInit {
  form: FormGroup;
  establishments: Establishment[] = [];

  // ✅ OPCIÓN A: Soporte para super admin
  isSuperAdmin = false;
  companies: Company[] = [];

  // ✅ Bandera para evitar sobrescribir cambios del usuario
  private formInitialized = false;

  constructor(
    private readonly issuancePointService: IssuancePointService,
    private readonly establishmentService: EstablishmentService,
    private readonly companyService: CompanyService, // ✅ Nuevo servicio
    private readonly logger: LoggerService,
    private readonly toast: ToastService,
    private readonly elementRef: ElementRef, // ✅ Para manipular DOM
    public dialogRef: MatDialogRef<UpdateIssuancePointComponent>,
    @Inject(MAT_DIALOG_DATA) public data: IssuancePointDialogData
  ) {}

  ngOnInit(): void {
    // ✅ Detectar si es super admin
    this.isSuperAdmin = this.data.isSuperAdmin || false;

    // Crear form base
    this.form = new FormGroup({
      code: new FormControl('', [
        Validators.required,
        Validators.pattern(/^[0-9]{3}$/),
      ]),
      establishmentId: new FormControl('', Validators.required),
      sequentialInvoice: new FormControl(1, [
        Validators.required,
        Validators.min(1),
      ]),
      status: new FormControl(true),
    });

    // ✅ SI ES SUPER ADMIN Y ES CREACIÓN: Agregar campo companyId
    if (this.isSuperAdmin && !this.data.point) {
      this.form.addControl(
        'companyId',
        new FormControl(this.data.idCompany || '', Validators.required)
      );
      this.loadCompanies();

      // ✅ Escuchar cambios en companyId para cargar sucursales
      this.form.get('companyId')?.valueChanges.subscribe((companyId) => {
        const id = companyId != null && companyId !== '' ? Number(companyId) : null;
        if (id != null && !isNaN(id)) {
          this.loadEstablishments(id);
          this.form.patchValue({ establishmentId: '' });
        } else {
          this.establishments = [];
        }
      });
    } else if (this.isSuperAdmin && this.data.point) {
      // ✅ Super Admin en EDICIÓN: Cargar compañías y sucursales pero NO permitir cambiar compañía
      this.loadCompanies();
    } else {
      // ✅ Company Admin: Cargar sucursales de su compañía
      this.loadEstablishments(this.data.idCompany!);
    }

    if (this.data.point) {
      this.initForm();
    }
  }

  /**
   * ✅ Ya no necesitamos ngAfterViewInit - usamos checkbox nativo
   */
  ngAfterViewInit(): void {
    // Checkbox nativo HTML - no requiere manipulación del DOM
  }

  /**
   * ✅ OPCIÓN A: Cargar compañías (solo para super admin)
   */
  loadCompanies(): void {
    this.companyService.findAll().subscribe({
      next: (response) => {
        this.companies = (response.companyDetailsDtos || []).filter(
          (c: Company) => c.status
        );
        this.logger.debug('Compañías activas cargadas:', this.companies.length);

        // ✅ Si es creación y ya hay compañía seleccionada (p. ej. desde filtros admin), cargar sus sucursales
        if (this.isSuperAdmin && !this.data.point) {
          const initialCompanyId = this.form.get('companyId')?.value;
          const id = initialCompanyId != null && initialCompanyId !== '' ? Number(initialCompanyId) : null;
          if (id != null && !isNaN(id)) {
            this.loadEstablishments(id);
          }
        }

        // ✅ Si es edición, cargar sucursales de la compañía del punto
        if (this.data.point) {
          const companyId =
            this.data.idCompany || (this.data.point as any).idCompany;
          if (companyId) {
            // ✅ Cargar sucursales y LUEGO setear establishmentId + status
            this.loadEstablishments(companyId).then(() => {
              // Después de cargar sucursales, setear establishmentId
              if (this.data.point && !this.formInitialized) {
                const normalizedStatus = Boolean(this.data.point.status);
                this.form.patchValue({
                  establishmentId: this.data.point.establishmentId,
                  status: normalizedStatus,
                });

                // ✅ Marcar como inicializado DESPUÉS de setear todos los valores
                this.formInitialized = true;

                this.logger.debug(
                  `[PUNTO EMISIÓN] Form inicializado - Status: ${this.data.point.status} → ${normalizedStatus}`
                );
              }
            });
          }
        }
      },
      error: (err) => {
        this.logger.error('Error al cargar compañías', err);
        this.toast.httpError(err, 'cargar compañías');
      },
    });
  }

  /**
   * ✅ OPCIÓN A: Cargar sucursales de una compañía
   * @param idCompany - ID de la compañía (requerido para super admin, opcional para company admin)
   * @returns Promise que se resuelve cuando las sucursales están cargadas
   */
  loadEstablishments(idCompany?: number): Promise<void> {
    return new Promise((resolve, reject) => {
      const companyId = idCompany || this.data.idCompany;
      if (!companyId) {
        this.logger.warn('No se proporcionó idCompany para cargar sucursales');
        reject('No companyId provided');
        return;
      }

      this.establishmentService.findByCompany(companyId).subscribe({
        next: (data) => {
          const raw = data || [];
          this.establishments = raw.filter((e: Establishment) => {
            const s = e.status;
            if (s === undefined || s === null) return true;
            if (typeof s === 'boolean') return s;
            if (typeof s === 'number') return s !== 0;
            if (typeof s === 'string') return s === 'true' || s === '1' || s === 'ACTIVE' || s === 'activo';
            return Boolean(s);
          });
          this.logger.debug(
            `Sucursales activas cargadas (Compañía ${companyId}):`,
            this.establishments.length,
            'raw:',
            raw.length
          );
          resolve(); // ✅ Resolver la promesa cuando termine
        },
        error: (err) => {
          this.logger.error('Error al cargar sucursales', err);
          this.toast.httpError(err, 'cargar sucursales');
          reject(err);
        },
      });
    });
  }

  /**
   * ✅ MEJORA: Helper para normalizar valores falsy problemáticos
   * Evita que 0, false, '' sean tratados incorrectamente
   */
  private normalizeValue<T>(value: T, defaultValue: T): T {
    return value ?? defaultValue;
  }

  initForm(): void {
    const point = this.data.point!;

    // ✅ Normalizar campos para evitar valores falsy problemáticos
    const normalizedStatus = Boolean(point.status);
    // El secuencial mínimo es 1, si es 0 o null/undefined, usar 1
    const sequentialValue = this.normalizeValue(point.sequentialInvoice, 1);
    // Asegurar que si el valor es 0, se cambie a 1
    const finalSequentialValue = sequentialValue === 0 ? 1 : sequentialValue;

    this.form.patchValue({
      code: point.code,
      establishmentId: point.establishmentId,
      sequentialInvoice: finalSequentialValue,
      status: normalizedStatus,
    });

    // ✅ Para company admin (NO super admin), cargar sucursales inmediatamente
    if (!this.isSuperAdmin) {
      this.loadEstablishments(this.data.idCompany!);
    }
  }

  save(): void {
    if (this.form.invalid) {
      // ✅ MEJORA: Marcar todos los campos como touched para mostrar errores
      Object.keys(this.form.controls).forEach((key) => {
        const control = this.form.get(key);
        control?.markAsTouched();
      });

      this.toast.warning('Por favor, corrija los errores en el formulario');
      return;
    }

    const request: IssuancePointRequest = {
      code: this.form.value.code,
      establishmentId: this.form.value.establishmentId,
      sequentialInvoice: this.form.value.sequentialInvoice,
      status: this.form.value.status,
    };

    this.logger.debug('📤 Enviando request:', JSON.stringify(request));

    if (this.data.point) {
      // Actualizar
      this.issuancePointService
        .update(this.data.point.idInvoiceIssuancePoint, request)
        .subscribe({
          next: () => {
            this.toast.success('¡Punto de emisión actualizado exitosamente!');
            this.dialogRef.close(true);
          },
          error: (err) => {
            this.logger.error('Error al actualizar punto de emisión', err);
            this.toast.httpError(err, 'actualizar el punto de emisión');
          },
        });
    } else {
      // Crear
      this.issuancePointService.create(request).subscribe({
        next: () => {
          this.toast.success('¡Punto de emisión creado exitosamente!');
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.logger.error('Error al crear punto de emisión', err);
          this.toast.httpError(err, 'crear el punto de emisión');
        },
      });
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
