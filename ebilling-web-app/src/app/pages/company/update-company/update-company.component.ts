import {
  Component,
  OnInit,
  Inject,
  ChangeDetectorRef,
  AfterViewInit,
  OnDestroy,
} from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import {
  EXIST_DATA,
  SUCCESS,
  UPDATE,
} from '../../../_constants/constants';
import {
  CompanyFilter,
  TAX_REGIME_OPTIONS,
  TaxRegimeOption,
} from '../../../_model/customer/company-filter';
import { CompanyService } from '../../../_service/customer/company.service';
import { HttpClient } from '@angular/common/http';
import { SubscriptionAdminService } from '../../../_service/subscription/subscription-admin.service';
import { AvailablePlan } from '../../../_model/subscription/subscription-admin';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-update-company',
  templateUrl: './update-company.component.html',
  styleUrls: ['./update-company.component.css'],
  animations: [
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)' }),
        animate(
          '300ms ease-out',
          style({ opacity: 1, transform: 'translateY(0)' })
        ),
      ]),
    ]),
  ],
})
export class UpdateCompanyComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  idCompany: number | null = null;
  company: CompanyFilter = {} as CompanyFilter;
  form: FormGroup;
  edition = false;
  identification = '';
  name = '';
  nameCommercial = '';
  forceToAccounting = true;
  selectedFile: File | null = null;
  logoPreview: string | null = null;
  logoPath: string | null = null;
  logoBlobUrl: string | null = null;
  /** True cuando el usuario hace clic en "Quitar logo" (solo en edición). Al guardar se llama deleteLogo y se actualiza la compañía sin logo. */
  userRemovedLogo = false;
  taxRegimeOptions: TaxRegimeOption[] = TAX_REGIME_OPTIONS;

  // Campos para suscripción (solo en modo creación)
  availablePlans: AvailablePlan[] = [];
  isLoadingPlans = false;

  constructor(
    private readonly companyService: CompanyService,
    public dialogRef: MatDialogRef<UpdateCompanyComponent>,
    private readonly cdr: ChangeDetectorRef,
    private readonly http: HttpClient,
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly logger: LoggerService,
    private readonly toast: ToastService,
    @Inject(MAT_DIALOG_DATA)
    public data: { company?: CompanyFilter; isEdit: boolean }
  ) {}

  ngOnInit(): void {
    this.company = {} as CompanyFilter;
    this.edition = this.data.isEdit;

    // Inicializar el form SIEMPRE primero
    const today = new Date();

    this.form = new FormGroup({
      id: new FormControl(null),
      identification: new FormControl('', [
        Validators.required,
        Validators.pattern(/^\d{13}$/),
      ]),
      name: new FormControl('', Validators.required),
      nameCommercial: new FormControl(''),
      status: new FormControl(true),
      forceToAccounting: new FormControl(true),
      regime: new FormControl('', Validators.required), // Campo para el régimen tributario (obligatorio)
      specialTaxpayerResolution: new FormControl({ value: '', disabled: true }), // Campo para la resolución (deshabilitado por defecto)

      // Campos de suscripción (solo para creación)
      planCode: new FormControl('FREE', Validators.required),
      startDate: new FormControl(today, Validators.required), // Date object para datepicker
      durationMonths: new FormControl(12, [
        Validators.required,
        Validators.min(1),
        Validators.max(60),
      ]),
    });

    // Suscribirse a cambios en el campo regime para habilitar/deshabilitar specialTaxpayerResolution
    this.form.get('regime')?.valueChanges.subscribe((regimeValue) => {
      const resolutionControl = this.form.get('specialTaxpayerResolution');
      if (regimeValue === 'SPECIAL') {
        resolutionControl?.enable();
      } else {
        resolutionControl?.disable();
        resolutionControl?.setValue('', { emitEvent: false }); // Limpiar el valor si no es SPECIAL
      }
    });

    // Si es creación (no edición), deshabilitar el campo status
    // El valor por defecto es true, pero no se puede cambiar al crear
    if (!this.edition) {
      const statusControl = this.form.get('status');
      if (statusControl) {
        // Asegurar que el valor sea true
        statusControl.setValue(true, { emitEvent: false });
        statusControl.disable();
        this.cdr.detectChanges();
      }

      // Cargar planes disponibles para asignación de suscripción
      this.loadAvailablePlans();
    } else {
      // En modo edición, deshabilitar campos de suscripción
      this.form.get('planCode')?.disable();
      this.form.get('startDate')?.disable();
      this.form.get('durationMonths')?.disable();
    }

    // Si hay datos, cargarlos en el form
    if (
      this.edition &&
      this.data.company &&
      this.data.company.idCompany !== null
    ) {
      this.idCompany = this.data.company.idCompany;
      this.initForm();
      // Cargar logo existente después de que el formulario esté inicializado
      // Usar setTimeout para asegurar que el formulario tenga los valores cargados
      setTimeout(() => {
        this.loadExistingLogo();
      }, 100);
    }
  }

  initForm() {
    if (this.edition && this.data.company && this.idCompany !== null) {
      const company = this.data.company;
      this.identification = company.identification;
      this.name = company.name;
      this.nameCommercial = company.nameCommercial || '';
      this.forceToAccounting = company.forcedToAccounting === 'S';

      // Usar patchValue() para cargar los datos
      this.form.patchValue({
        id: this.idCompany, // Usar this.idCompany que ya sabemos que no es null
        identification: company.identification,
        name: company.name,
        nameCommercial: company.nameCommercial || '',
        status: company.status,
        forceToAccounting: company.forcedToAccounting === 'S',
        regime: company.regime || '', // Cargar régimen si existe
        specialTaxpayerResolution: company.specialTaxpayerResolution || '', // Cargar resolución si existe
      });

      // Habilitar/deshabilitar el campo de resolución basado en el régimen
      const regimeValue = company.regime || '';
      const resolutionControl = this.form.get('specialTaxpayerResolution');
      if (regimeValue === 'SPECIAL') {
        resolutionControl?.enable();
      } else {
        resolutionControl?.disable();
      }

      // Forzar la detección de cambios
      this.cdr.detectChanges();
    }
  }

  ngAfterViewInit(): void {
    // Este método puede ser utilizado para inicializaciones adicionales después de que la vista esté completamente renderizada
  }

  validateSave() {
    if (
      this.edition &&
      this.identification === this.form.value['identification']
    ) {
      this.save();
    } else {
      this.validateIdentification();
    }
  }

  validateIdentification() {
    this.companyService
      .existsByIdentification(this.form.value['identification'])
      .subscribe((data) => {
        if (data) {
          this.toast.info(
            EXIST_DATA.MESSAGE_EXISTS_IDENTIFICATION.message
          );
        } else {
          this.save();
        }
      });
  }

  save() {
    // Inicializar el objeto company con todas las propiedades necesarias
    const resolvedType = this.data.company?.type || 'MATRIZ';
    const resolvedPrincipal = resolvedType === 'MATRIZ' ? 'S' : 'N';

    // Obtener todos los valores del formulario (incluyendo campos deshabilitados)
    const formValues = this.form.getRawValue();

    if (this.edition && this.idCompany !== null) {
      // Actualización: incluir idCompany en el payload
      const companyId: number = this.idCompany; // Type guard para TypeScript

      if (this.userRemovedLogo) {
        // Usuario eligió quitar el logo: eliminar en backend y luego actualizar compañía sin logo
        this.companyService.deleteLogo(companyId).subscribe({
          next: () => {
            this.updateCompanyWithLogoPath(
              companyId,
              formValues,
              resolvedType,
              resolvedPrincipal,
              null
            );
          },
          error: (error) => {
            this.toast.httpError(error, 'eliminar el logo');
          },
        });
        return;
      }

      // Si no hay un archivo nuevo seleccionado, obtener el logoPath existente del servidor
      if (!this.selectedFile) {
        // Obtener el logoPath existente del servidor antes de actualizar
        this.companyService.getLogoPath(companyId).subscribe({
          next: (response: any) => {
            const existingLogoPath = response?.logoPath || null;
            this.updateCompanyWithLogoPath(
              companyId,
              formValues,
              resolvedType,
              resolvedPrincipal,
              existingLogoPath
            );
          },
          error: (error) => {
            this.updateCompanyWithLogoPath(
              companyId,
              formValues,
              resolvedType,
              resolvedPrincipal,
              null
            );
          },
        });
      } else {
        // Si hay un archivo nuevo, no incluir logoPath (se actualizará después al subir el archivo)
        this.updateCompanyWithLogoPath(
          companyId,
          formValues,
          resolvedType,
          resolvedPrincipal,
          null
        );
      }
    } else {
      // Creación: NO incluir idCompany, el backend lo genera automáticamente
      // Usar getRawValue() para obtener el valor de status incluso si está deshabilitado
      const companyForCreate = {
        idType: this.data.company?.idType || 0,
        identification: formValues['identification'],
        name: formValues['name'],
        nameCommercial: formValues['nameCommercial'],
        status: !!formValues['status'] || true, // Asegurar que sea true por defecto si está deshabilitado
        forcedToAccounting: formValues['forceToAccounting'] ? 'S' : 'N',
        type: resolvedType,
        principal: resolvedPrincipal,
        regime: formValues['regime'] || '', // Incluir régimen tributario (obligatorio)
        specialTaxpayerResolution:
          formValues['regime'] === 'SPECIAL'
            ? formValues['specialTaxpayerResolution'] || ''
            : '', // Solo incluir si es SPECIAL
        isSuperAdmin: false,
      };

      // Enviar sin idCompany - el backend lo genera automáticamente
      this.companyService.save(companyForCreate as any).subscribe({
        next: (response: any) => {
          this.logger.debug('[CompanyComponent] Respuesta al crear compañía:', response);
          const newIdCompany = response?.idCompany || response?.id;

          if (!newIdCompany) {
            this.logger.error('[CompanyComponent] No se obtuvo ID de la compañía creada');
            this.toast.error(
              'Error: No se pudo obtener el ID de la compañía creada'
            );
            return;
          }

          // Asignar suscripción después de crear la compañía
          this.assignSubscriptionToCompany(newIdCompany, formValues)
            .then(() => {
              // Después de asignar suscripción, subir logo si hay archivo
              if (this.selectedFile) {
                this.uploadLogo(newIdCompany);
              } else {
                this.toast.success(
                  'Compañía creada y suscripción asignada exitosamente'
                );
                this.dialogRef.close(true);
              }
            })
            .catch((error) => {
              // Si falla la asignación de suscripción, aún así cerrar el modal
              // El usuario puede asignar la suscripción manualmente después
              this.logger.error('[CompanyComponent] Error al asignar suscripción', error);
              this.toast.warning(
                'Compañía creada, pero no se pudo asignar la suscripción. Puedes asignarla manualmente después.'
              );

              // Subir logo si hay archivo
              if (this.selectedFile) {
                this.uploadLogo(newIdCompany);
              } else {
                this.dialogRef.close(true);
              }
            });
        },
        error: (error) => {
          this.logger.error('Error al crear la compañía', error);
          this.toast.httpError(error, 'crear la compañía');
        },
      });
    }
  }

  /**
   * Actualiza la compañía incluyendo el logoPath existente (si no se está subiendo un archivo nuevo)
   */
  private updateCompanyWithLogoPath(
    companyId: number,
    formValues: any,
    resolvedType: string,
    resolvedPrincipal: string,
    existingLogoPath: string | null
  ): void {
    this.company = {
      idCompany: companyId,
      idType: this.data.company?.idType || 0,
      identification: formValues['identification'],
      name: formValues['name'],
      nameCommercial: formValues['nameCommercial'],
      status: !!formValues['status'], // Asegurar que sea booleano
      forcedToAccounting: formValues['forceToAccounting'] ? 'S' : 'N',
      type: resolvedType,
      principal: resolvedPrincipal,
      regime: formValues['regime'] || '', // Incluir régimen tributario (obligatorio)
      specialTaxpayerResolution:
        formValues['regime'] === 'SPECIAL'
          ? formValues['specialTaxpayerResolution'] || ''
          : '', // Solo incluir si es SPECIAL
      // Incluir logoPath solo si no se está subiendo un archivo nuevo ni el usuario quitó el logo
      logoPath:
        this.selectedFile || this.userRemovedLogo
          ? undefined
          : existingLogoPath ?? undefined,
      isSuperAdmin: false,
    };

    this.companyService.update(this.company, companyId).subscribe({
      next: () => {
        // Si hay un archivo seleccionado, subirlo usando el ID de la compañía
        if (this.selectedFile) {
          this.uploadLogo(companyId);
        } else {
          // ✅ Mostrar toast personalizado de éxito
          this.toast.success(UPDATE.MESSAGE_UPDATE_COMPANY.message);
          this.dialogRef.close(true);
        }
      },
      error: (error) => {
        this.toast.httpError(error, 'actualizar la compañía');
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false); // Cierra el modal sin cambios
  }

  /**
   * Quita el logo en la UI y marca que al guardar se debe eliminar en el backend (solo en edición).
   */
  onRemoveLogo(): void {
    this.userRemovedLogo = true;
    this.selectedFile = null;
    this.logoPreview = null;
    this.logoPath = null;
    if (this.logoBlobUrl) {
      URL.revokeObjectURL(this.logoBlobUrl);
      this.logoBlobUrl = null;
    }
    this.cdr.detectChanges();
  }

  /**
   * Maneja la selección de archivo de logo
   */
  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validar tipo de archivo
      if (!file.type.startsWith('image/')) {
        this.toast.error('El archivo debe ser una imagen');
        return;
      }

      // Validar tamaño (máximo 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.toast.error('El archivo no debe exceder 5MB');
        return;
      }

      this.selectedFile = file;
      this.userRemovedLogo = false;

      // Mostrar preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.logoPreview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  /**
   * Sube el logo de la compañía usando el ID
   */
  uploadLogo(idCompany: number): void {
    if (!this.selectedFile || !idCompany) {
      this.logger.error('[CompanyComponent] No se puede subir el logo', { idCompany });
      return;
    }

    this.logger.debug('[CompanyComponent] Subiendo logo para compañía ID:', { idCompany });
    this.companyService.uploadLogo(idCompany, this.selectedFile).subscribe({
      next: () => {
        this.logger.debug('[CompanyComponent] Logo subido exitosamente');
        this.toast.success(
          this.edition
            ? UPDATE.MESSAGE_UPDATE_COMPANY.message
            : SUCCESS.MESSAGE_REGISTER_COMPANY.message
        );
        this.dialogRef.close(true);
      },
      error: (error) => {
        this.logger.error('[CompanyComponent] Error al subir logo', error);
        this.toast.httpError(error, 'subir el logo');
      },
    });
  }

  /**
   * Carga el logo existente al editar
   * Intenta cargar directamente la imagen. Si no existe (404), simplemente no se muestra.
   */
  loadExistingLogo(): void {
    // Usar el ID de la compañía para cargar el logo
    if (this.idCompany !== null) {
      const companyId = this.idCompany; // Type guard para TypeScript
      // Cargar directamente la imagen como blob
      // Si no existe, el error 404 se maneja silenciosamente
      this.loadLogoAsBlob(companyId);
    }
  }

  /**
   * Carga el logo como blob para incluir el token JWT en la petición
   */
  loadLogoAsBlob(idCompany: number): void {
    const logoUrl = this.companyService.getLogoUrl(idCompany);

    // Usar HttpClient para obtener la imagen como blob (incluye JWT automáticamente)
    this.http.get(logoUrl, { responseType: 'blob' }).subscribe({
      next: (blob: Blob) => {
        // Crear una URL local para el blob
        if (this.logoBlobUrl) {
          URL.revokeObjectURL(this.logoBlobUrl); // Limpiar URL anterior si existe
        }
        this.logoBlobUrl = URL.createObjectURL(blob);
        this.logoPath = this.logoBlobUrl;
        this.cdr.detectChanges();
        this.logger.debug('[CompanyComponent] Logo cargado como blob');
      },
      error: (error) => {
        if (error.status === 404) {
          this.logger.debug('[CompanyComponent] Logo no encontrado (404)', { idCompany });
          this.logoPath = null;
          this.logoBlobUrl = null;
        } else {
          this.logger.error('[CompanyComponent] Error al cargar logo', error);
          this.logoPath = null;
          this.logoBlobUrl = null;
        }
      },
    });
  }

  /**
   * Limpia las URLs de blob al destruir el componente
   */
  ngOnDestroy(): void {
    if (this.logoBlobUrl) {
      URL.revokeObjectURL(this.logoBlobUrl);
    }
  }

  /**
   * Maneja errores al cargar la imagen del logo
   */
  onImageError(event: any): void {
    this.logger.warn('[CompanyComponent] Error al cargar imagen del logo', event);
    // Si hay un error, intentar cargar sin el timestamp
    if (this.logoPath && this.logoPath.includes('?t=')) {
      const baseUrl = this.logoPath.split('?')[0];
      this.logoPath = baseUrl;
    } else {
      // Si aún falla, ocultar la imagen
      this.logoPath = null;
    }
  }

  /**
   * Maneja la carga exitosa de la imagen
   */
  onImageLoad(): void {}

  /**
   * Carga los planes disponibles para asignación de suscripción
   */
  private loadAvailablePlans(): void {
    this.isLoadingPlans = true;
    this.subscriptionService.getAvailablePlans().subscribe({
      next: (plans) => {
        this.availablePlans = plans;
        this.isLoadingPlans = false;
        this.logger.info('Planes disponibles cargados', plans);
      },
      error: (error: HttpErrorResponse) => {
        this.logger.error('Error al cargar planes disponibles', error);
        this.toast.httpError(error, 'cargar los planes disponibles');
        this.isLoadingPlans = false;
      },
    });
  }

  /**
   * Obtiene el plan seleccionado
   */
  get selectedPlan(): AvailablePlan | undefined {
    const planCode = this.form.get('planCode')?.value;
    return this.availablePlans.find((p) => p.code === planCode);
  }

  /**
   * Obtiene el texto formateado del plan seleccionado para mostrar en el select (colapsado)
   * Formato: "Plan Básico • $29.99/mes • 500 doc/año"
   */
  getSelectedPlanDisplay(): string {
    const plan = this.selectedPlan;
    if (!plan) return '';

    const parts: string[] = [];

    // Nombre del plan
    parts.push(plan.name);

    // Precio mensual
    parts.push(`$${plan.price.toFixed(2)}/mes`);

    // Límite de documentos
    if (plan.documentLimit === -1) {
      parts.push('Ilimitado');
    } else {
      parts.push(`${plan.documentLimit.toLocaleString()} doc/año`);
    }

    // Unir con separador " • " (bullet point con espacios)
    return parts.join(' • ');
  }

  /**
   * Calcula la fecha de finalización basada en fecha de inicio y duración
   */
  get calculatedEndDate(): Date | null {
    const startDate = this.form.get('startDate')?.value;
    const durationMonths = this.form.get('durationMonths')?.value;

    if (!startDate || !durationMonths) {
      return null;
    }

    // startDate puede ser Date object o string ISO
    const start = startDate instanceof Date ? startDate : new Date(startDate);
    const endDate = new Date(start);
    endDate.setMonth(endDate.getMonth() + durationMonths);
    return endDate;
  }

  /**
   * Calcula el costo total del período
   */
  get totalCost(): number {
    const plan = this.selectedPlan;
    const durationMonths = this.form.get('durationMonths')?.value || 0;

    if (!plan || !durationMonths) {
      return 0;
    }

    return plan.price * durationMonths;
  }

  /**
   * Asigna la suscripción a la compañía recién creada
   */
  private async assignSubscriptionToCompany(
    idCompany: number,
    formValues: any
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      // Convertir fecha Date a string ISO (YYYY-MM-DD)
      let startDateValue = formValues['startDate'];
      if (startDateValue instanceof Date) {
        startDateValue = startDateValue.toISOString().split('T')[0];
      } else if (typeof startDateValue === 'string') {
        // Si ya es string, asegurar formato YYYY-MM-DD
        startDateValue = startDateValue.split('T')[0];
      }

      const subscriptionRequest = {
        idCompany: idCompany,
        planCode: formValues['planCode'] || 'FREE',
        startDate: startDateValue,
        durationMonths: formValues['durationMonths'] || 12,
      };

      this.logger.info(
        '📤 Asignando suscripción a compañía:',
        subscriptionRequest
      );

      this.subscriptionService
        .assignSubscription(subscriptionRequest)
        .subscribe({
          next: (response) => {
            this.logger.info('Suscripción asignada exitosamente', response);
            resolve();
          },
          error: (error: HttpErrorResponse) => {
            this.logger.error('Error al asignar suscripción', error);
            reject(error);
          },
        });
    });
  }
}
