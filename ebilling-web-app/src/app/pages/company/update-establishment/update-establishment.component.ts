import {
  Component,
  OnInit,
  Inject,
  ChangeDetectorRef,
  AfterViewInit,
} from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {
  MatDialogRef,
  MAT_DIALOG_DATA,
  MatDialog,
} from '@angular/material/dialog';
import {
  Establishment,
  Company,
} from '../../../_model/customer/company-establishment';
import {
  EstablishmentService,
  EstablishmentRequest,
} from '../../../_service/customer/establishment.service';
import { CompanyService } from '../../../_service/customer/company.service';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../_service/notification/toast.service';

export interface EstablishmentDialogData {
  establishment?: Establishment;
  idCompany?: number; // ✅ Ahora es opcional
  isSuperAdmin?: boolean; // ✅ OPCIÓN A: Flag para super admin
}

@Component({
  selector: 'app-update-establishment',
  templateUrl: './update-establishment.component.html',
  styleUrls: ['./update-establishment.component.css'],
})
export class UpdateEstablishmentComponent implements OnInit, AfterViewInit {
  form: FormGroup;
  private existingMatriz: Establishment | null = null;
  private userConfirmedMatrizChange = false;
  isProcessingMatrizChange = false;

  // ✅ OPCIÓN A: Soporte para super admin
  isSuperAdmin = false;
  companies: Company[] = [];

  constructor(
    private readonly establishmentService: EstablishmentService,
    private readonly companyService: CompanyService, // ✅ Nuevo servicio
    private readonly logger: LoggerService,
    private readonly toast: ToastService,
    private readonly dialog: MatDialog,
    private readonly cdr: ChangeDetectorRef, // ✅ Para forzar detección de cambios
    public dialogRef: MatDialogRef<UpdateEstablishmentComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EstablishmentDialogData
  ) {}

  ngOnInit(): void {
    // ✅ Detectar si es super admin
    this.isSuperAdmin = this.data.isSuperAdmin || false;

    // ✅ Obtener el companyId inicial (del establishment si existe, sino del data)
    const initialCompanyId =
      this.data.establishment?.idCompany || this.data.idCompany || null;

    // Crear form base
    this.form = new FormGroup({
      code: new FormControl('', [
        Validators.required,
        Validators.pattern(/^[0-9]{3}$/),
      ]),
      name: new FormControl('', Validators.required),
      address: new FormControl('', Validators.required),
      type: new FormControl('SUCURSAL', Validators.required),
      status: new FormControl(true),
    });

    // ✅ SI ES SUPER ADMIN: Agregar campo companyId
    if (this.isSuperAdmin) {
      // Convertir a número si existe
      const initialCompanyIdNumber = initialCompanyId
        ? Number(initialCompanyId)
        : null;
      this.form.addControl(
        'companyId',
        new FormControl(initialCompanyIdNumber || null, Validators.required)
      );
      this.loadCompanies();
    }

    if (this.data.establishment) {
      this.initForm();
    }

    // Escuchar cambios en el campo 'type'
    this.form.get('type')?.valueChanges.subscribe((newType) => {
      if (newType === 'MATRIZ') {
        this.checkExistingMatriz();
      }
    });
  }

  ngAfterViewInit(): void {
    // ✅ Establecer el companyId después de que la vista esté completamente inicializada
    // Esto se ejecuta como respaldo si las compañías ya están cargadas
    if (this.isSuperAdmin && this.data.establishment) {
      const companyIdValue =
        this.data.establishment.idCompany || this.data.idCompany;
      if (companyIdValue && this.companies.length > 0) {
        const companyIdNumber = Number(companyIdValue);
        const companyControl = this.form.get('companyId');

        if (
          companyControl &&
          this.companies.some((c) => c.idCompany === companyIdNumber)
        ) {
          // Usar setTimeout para asegurar que el mat-select esté completamente renderizado
          setTimeout(() => {
            const currentValue = companyControl.value;
            if (currentValue !== companyIdNumber) {
              companyControl.setValue(companyIdNumber, {
                emitEvent: false,
                onlySelf: false,
              });
              this.logger.debug(
                `[ADMIN] CompanyId establecido en AfterViewInit: ${companyIdNumber}, Control value: ${companyControl.value}`
              );
              this.cdr.detectChanges();
            }
          }, 500); // Timeout más largo para asegurar que todo esté renderizado
        }
      }
    }
  }

  initForm(): void {
    const est = this.data.establishment!;

    // ✅ OPTIMIZACIÓN: Normalizar status a boolean explícito
    const normalizedStatus = Boolean(est.status);

    // ✅ Preparar los valores para patchValue
    const formValues: any = {
      code: est.code,
      name: est.name,
      address: est.address,
      type: est.type,
      status: normalizedStatus,
    };

    // ✅ NO establecer companyId aquí - se establecerá en loadCompanies() después de cargar las compañías
    // Esto evita problemas de timing cuando las compañías aún no están disponibles

    this.form.patchValue(formValues);

    // ✅ Log para depuración
    const companyIdValue = est.idCompany || this.data.idCompany || null;
    this.logger.debug(
      `[SUCURSAL] Formulario inicializado - Status: ${
        est.status
      } → ${normalizedStatus}, CompanyId (del establishment): ${
        companyIdValue || 'N/A'
      }, FormValid: ${this.form.valid}, FormInvalid: ${this.form.invalid}`
    );

    // ✅ Si el formulario es inválido, log de los errores
    if (this.form.invalid) {
      const errors: any = {};
      Object.keys(this.form.controls).forEach((key) => {
        const control = this.form.get(key);
        if (control && control.invalid) {
          errors[key] = control.errors;
        }
      });
      this.logger.debug(`[SUCURSAL] Errores del formulario:`, errors);
    }
  }

  /**
   * Maneja el cambio del checkbox de status
   */
  onStatusChange(event: Event): void {
    event.stopPropagation(); // Evitar propagación adicional
    const target = event.target as HTMLInputElement;
    const newValue = target.checked;

    // Actualizar el FormControl con el nuevo valor explícitamente
    const statusControl = this.form.get('status');
    if (statusControl) {
      // Usar setValue con todas las opciones para asegurar la actualización
      statusControl.setValue(newValue, {
        emitEvent: true,
        onlySelf: false,
      });

      // Forzar la actualización del estado del checkbox visual
      target.checked = newValue;

      // Marcar el control como touched y dirty para que Angular detecte el cambio
      statusControl.markAsTouched();
      statusControl.markAsDirty();

      // ✅ Actualizar el estado del formulario para que el botón se habilite
      this.form.updateValueAndValidity({ emitEvent: true, onlySelf: false });

      // ✅ Forzar detección de cambios para actualizar el estado del botón
      this.cdr.detectChanges();
    }

    this.logger.debug(
      `[SUCURSAL] Status cambiado - Checkbox: ${newValue}, FormControl: ${statusControl?.value}, FormValue: ${this.form.value.status}, FormValid: ${this.form.valid}, FormInvalid: ${this.form.invalid}`
    );
  }

  /**
   * ✅ Carga la lista de compañías (solo para super admin)
   */
  private loadCompanies(): void {
    this.companyService.findAll().subscribe({
      next: (response) => {
        this.companies = response.companyDetailsDtos || [];
        this.logger.info(
          `[ADMIN] ${this.companies.length} compañías cargadas para modal`
        );

        // ✅ Si estamos editando y hay un companyId, establecerlo después de cargar las compañías
        if (this.data.establishment && this.isSuperAdmin) {
          const companyIdValue =
            this.data.establishment.idCompany || this.data.idCompany;
          this.logger.debug(
            `[ADMIN] Verificando companyId - establishment.idCompany: ${
              this.data.establishment.idCompany
            }, data.idCompany: ${
              this.data.idCompany
            }, companyIdValue: ${companyIdValue}, isSuperAdmin: ${
              this.isSuperAdmin
            }, form tiene companyId: ${!!this.form.get('companyId')}`
          );
          if (
            companyIdValue !== null &&
            companyIdValue !== undefined &&
            this.form.get('companyId')
          ) {
            // Convertir a número para asegurar que coincida con el tipo de las opciones
            const companyIdNumber = Number(companyIdValue);

            // Verificar que la compañía existe en la lista cargada
            const companyExists = this.companies.some(
              (c) => c.idCompany === companyIdNumber
            );
            this.logger.debug(
              `[ADMIN] CompanyId a establecer: ${companyIdNumber}, Existe en lista: ${companyExists}, Total compañías: ${
                this.companies.length
              }, IDs: ${this.companies.map((c) => c.idCompany).join(', ')}`
            );

            if (companyExists) {
              // Establecer el valor inmediatamente después de cargar las compañías
              const companyControl = this.form.get('companyId');
              if (companyControl) {
                // Primero establecer el valor
                companyControl.setValue(companyIdNumber, {
                  emitEvent: false,
                  onlySelf: false,
                });
                this.logger.debug(
                  `[ADMIN] CompanyId establecido inmediatamente: ${companyIdNumber}, Control value: ${companyControl.value}`
                );

                // Forzar detección de cambios
                this.cdr.detectChanges();

                // Usar setTimeout para asegurar que el mat-select esté completamente renderizado
                // y que las opciones estén disponibles
                setTimeout(() => {
                  // Verificar y re-establecer el valor si es necesario
                  const currentValue = companyControl.value;
                  if (currentValue !== companyIdNumber) {
                    companyControl.setValue(companyIdNumber, {
                      emitEvent: false,
                      onlySelf: false,
                    });
                    this.logger.debug(
                      `[ADMIN] CompanyId re-establecido (timeout 500ms): ${companyIdNumber}, Control value: ${companyControl.value}`
                    );
                    this.cdr.detectChanges();
                  }

                  // Tercer intento después de un delay adicional
                  setTimeout(() => {
                    const finalValue = companyControl.value;
                    if (finalValue !== companyIdNumber) {
                      companyControl.setValue(companyIdNumber, {
                        emitEvent: false,
                        onlySelf: false,
                      });
                      this.logger.debug(
                        `[ADMIN] CompanyId establecido (timeout 700ms): ${companyIdNumber}, Control value: ${companyControl.value}`
                      );
                      this.cdr.detectChanges();
                    } else {
                      this.logger.debug(
                        `[ADMIN] CompanyId correctamente establecido: ${companyIdNumber}`
                      );
                    }
                  }, 200);
                }, 500); // Timeout inicial para asegurar que el mat-select esté completamente listo
              }
            } else {
              this.logger.warn(
                `[ADMIN] CompanyId ${companyIdNumber} no encontrado en la lista de compañías. IDs disponibles: ${this.companies
                  .map((c) => c.idCompany)
                  .join(', ')}`
              );
            }
          }
        }
      },
      error: (err) => {
        this.logger.error('[ADMIN] Error al cargar compañías', err);
        this.toast.httpError(err, 'cargar compañías');
      },
    });
  }

  /**
   * Verifica si ya existe una MATRIZ en la compañía
   */
  private checkExistingMatriz(): void {
    // Si el usuario ya confirmó el cambio, no volver a preguntar
    if (this.userConfirmedMatrizChange) {
      return;
    }

    // Si estamos editando y ya era MATRIZ, no preguntar
    if (this.data.establishment?.type === 'MATRIZ') {
      return;
    }

    // ✅ Obtener el ID de la compañía (del form si es super admin, sino del data)
    const companyId = this.isSuperAdmin
      ? this.form.get('companyId')?.value
      : this.data.idCompany;

    if (!companyId) {
      return;
    }

    // Buscar si existe una MATRIZ
    this.establishmentService.findByCompany(companyId).subscribe({
      next: (establishments) => {
        const matriz = establishments.find((e) => e.type === 'MATRIZ');

        if (matriz) {
          this.existingMatriz = matriz;
          this.showMatrizWarning(matriz);
        }
      },
      error: (err) => {
        this.logger.error('[MATRIZ] Error al verificar matriz existente', err);
      },
    });
  }

  /**
   * Muestra el warning sobre el cambio de matriz
   */
  private showMatrizWarning(currentMatriz: Establishment): void {
    // Escapar HTML para prevenir XSS
    const escapedName = this.escapeHtml(currentMatriz.name);
    const escapedCode = this.escapeHtml(currentMatriz.code);
    const escapedAddress = this.escapeHtml(currentMatriz.address);

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '580px',
      maxWidth: '95vw',
      disableClose: true,
      autoFocus: true,
      restoreFocus: true,
      data: {
        title: 'Cambiar Matriz',
        message: `
          <div style="text-align: left; line-height: 1.6;">
            <p style="margin: 0 0 16px 0; color: #666; font-size: 14px;">
              <strong style="color: #333; font-size: 15px;">Solo puede existir una MATRIZ por compañía.</strong>
            </p>
            
            <p style="margin: 0 0 8px 0; color: #666; font-size: 13px; font-weight: 500;">
              MATRIZ actual:
            </p>
            
            <div style="
              background: linear-gradient(135deg, #fff3cd 0%, #ffe9a0 100%);
              border-left: 4px solid #ff9800;
              padding: 16px;
              border-radius: 8px;
              margin: 0 0 20px 0;
              box-shadow: 0 2px 8px rgba(255, 152, 0, 0.15);
            ">
              <div style="display: flex; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 20px; margin-right: 8px;">🏢</span>
                <strong style="color: #333; font-size: 15px;">${escapedName}</strong>
              </div>
              <div style="color: #555; font-size: 13px; margin-left: 28px;">
                <div style="margin-bottom: 4px;">
                  <strong>Código:</strong> ${escapedCode}
                </div>
                <div>
                  <strong>Dirección:</strong> ${escapedAddress}
                </div>
              </div>
            </div>
            
            <div style="
              background: #f5f5f5;
              padding: 14px;
              border-radius: 8px;
              margin: 0 0 16px 0;
              border: 1px solid #e0e0e0;
            ">
              <p style="margin: 0 0 10px 0; color: #666; font-size: 13px;">
                <strong style="color: #333;">Si continúa:</strong>
              </p>
              <ol style="margin: 0; padding-left: 20px; color: #666; font-size: 13px; line-height: 1.8;">
                <li>
                  <strong style="color: #f57c00;">${escapedName}</strong> 
                  → <span style="color: #1976d2; font-weight: 600;">SUCURSAL</span>
                </li>
                <li>
                  Nueva sucursal → <span style="color: #f57c00; font-weight: 600;">MATRIZ</span>
                </li>
              </ol>
            </div>
            
            <p style="margin: 0; color: #666; font-size: 14px; text-align: center;">
              <strong style="color: #333;">¿Desea continuar con el cambio?</strong>
            </p>
          </div>
        `,
        confirmText: 'Sí, cambiar MATRIZ',
        cancelText: 'No, mantener actual',
        type: 'warning',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.userConfirmedMatrizChange = true;
        this.logger.info(
          `[MATRIZ] Cambio confirmado: ${currentMatriz.code} (${currentMatriz.name}) → SUCURSAL | Nueva → MATRIZ`
        );
      } else {
        // Revertir la selección a SUCURSAL
        this.form.patchValue({ type: 'SUCURSAL' }, { emitEvent: false });
        this.existingMatriz = null;
        this.userConfirmedMatrizChange = false;
      }
    });
  }

  /**
   * Escapa caracteres HTML para prevenir XSS
   */
  private escapeHtml(text: string): string {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  save(): void {
    if (this.form.invalid) {
      return;
    }

    // ✅ Obtener el ID de la compañía (del form si es super admin, sino del data)
    const companyIdRaw = this.isSuperAdmin
      ? this.form.get('companyId')?.value
      : this.data.idCompany;

    // ✅ Convertir a número y validar
    const companyId = companyIdRaw ? Number(companyIdRaw) : null;

    if (!companyId || isNaN(companyId)) {
      this.logger.error('[SUCURSAL] CompanyId inválido:', {
        raw: companyIdRaw,
        converted: companyId,
        isSuperAdmin: this.isSuperAdmin,
        formValue: this.form.get('companyId')?.value,
        dataIdCompany: this.data.idCompany,
      });
      this.toast.warning('Debe seleccionar una compañía');
      return;
    }

    this.logger.debug('[SUCURSAL] CompanyId obtenido:', {
      raw: companyIdRaw,
      converted: companyId,
      type: typeof companyId,
    });

    // ✅ Obtener el valor de status correctamente del FormControl
    // Leer directamente del FormControl para asegurar el valor más reciente
    const statusControl = this.form.get('status');
    const statusValue = statusControl?.value ?? false;

    // ✅ Asegurar que el valor sea explícitamente boolean
    const finalStatusValue =
      statusValue === true || statusValue === 'true' || statusValue === 1;

    // Usar getRawValue() para obtener los demás valores
    const formValues = this.form.getRawValue();

    // ✅ El backend espera 'idCompany' según la documentación, pero el servicio usa 'companyId'
    // Verificar qué espera realmente el backend
    const request: EstablishmentRequest = {
      code: formValues.code,
      name: formValues.name,
      address: formValues.address,
      companyId, // El servicio usa 'companyId', el backend podría esperar 'idCompany'
      type: formValues.type,
      status: finalStatusValue, // ✅ Valor boolean explícito
    };

    this.logger.debug(
      `[SUCURSAL] Guardando - Status del control: ${statusControl?.value}, Status (getRawValue): ${formValues.status}, Status final: ${request.status}`
    );

    // ✅ Log del objeto completo que se enviará
    this.logger.debug(
      `[SUCURSAL] Objeto request completo:`,
      JSON.stringify(request, null, 2)
    );

    // Si el usuario va a crear/actualizar como MATRIZ y existe otra MATRIZ
    if (
      this.form.value.type === 'MATRIZ' &&
      this.existingMatriz &&
      this.existingMatriz.idEstablishment !==
        this.data.establishment?.idEstablishment
    ) {
      // Indicar que está procesando el cambio
      this.isProcessingMatrizChange = true;

      // Mostrar feedback visual
      this.toast.info('Cambiando MATRIZ anterior a SUCURSAL...');

      // Primero cambiar la MATRIZ actual a SUCURSAL
      const updateOldMatrizRequest: EstablishmentRequest = {
        code: this.existingMatriz.code,
        name: this.existingMatriz.name,
        address: this.existingMatriz.address,
        companyId, // ✅ Usar el companyId correcto
        type: 'SUCURSAL',
        status: this.existingMatriz.status ?? true,
      };

      this.establishmentService
        .update(this.existingMatriz.idEstablishment, updateOldMatrizRequest)
        .subscribe({
          next: () => {
            this.logger.info(
              `[MATRIZ] ${this.existingMatriz?.code} cambiada: MATRIZ → SUCURSAL`
            );

            // Mostrar feedback de éxito del primer paso
            this.toast.success('MATRIZ anterior actualizada');

            // Ahora guardar la nueva MATRIZ
            this.saveEstablishment(request);
          },
          error: (err) => {
            this.isProcessingMatrizChange = false;
            this.logger.error(
              '[MATRIZ] Error al actualizar matriz anterior',
              err
            );
            this.toast.httpError(err, 'actualizar la matriz anterior');
          },
        });
    } else {
      // Guardar normalmente
      this.saveEstablishment(request);
    }
  }

  /**
   * Método auxiliar para guardar o actualizar una sucursal
   */
  private saveEstablishment(request: EstablishmentRequest): void {
    const action = this.data.establishment ? 'UPDATE' : 'CREATE';
    const id = this.data.establishment?.idEstablishment;

    // ✅ Log del objeto que se enviará al backend
    this.logger.debug(
      `[SUCURSAL] Enviando ${action} - Request:`,
      JSON.stringify(request, null, 2)
    );

    if (this.data.establishment) {
      // Actualizar
      this.establishmentService.update(id!, request).subscribe({
        next: () => {
          const successMsg = this.isProcessingMatrizChange
            ? 'Nueva MATRIZ establecida exitosamente'
            : 'Sucursal actualizada exitosamente';

          // Log de auditoría
          this.logger.info(
            `[SUCURSAL] ${action}: ${request.code} - ${request.name} (tipo: ${request.type})`
          );

          this.toast.success(successMsg);
          this.isProcessingMatrizChange = false;
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.isProcessingMatrizChange = false;
          this.logger.error(`[SUCURSAL] Error ${action}`, err);
          this.toast.httpError(err, 'actualizar la sucursal');
        },
      });
    } else {
      // Crear
      this.establishmentService.create(request).subscribe({
        next: () => {
          const successMsg = this.isProcessingMatrizChange
            ? 'Nueva MATRIZ creada exitosamente'
            : 'Sucursal creada exitosamente';

          // Log de auditoría
          this.logger.info(
            `[SUCURSAL] ${action}: ${request.code} - ${request.name} (tipo: ${request.type})`
          );

          this.toast.success(successMsg);
          this.isProcessingMatrizChange = false;
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.isProcessingMatrizChange = false;
          this.logger.error(`[SUCURSAL] Error ${action}`, err);

          // Log detallado del error completo para debugging
          console.error('[SUCURSAL] Error completo del backend:', {
            status: err.status,
            statusText: err.statusText,
            url: err.url,
            error: err.error,
            errorString: JSON.stringify(err.error, null, 2),
            errorType: typeof err.error,
            errorKeys: err.error ? Object.keys(err.error) : [],
          });

          // Extraer mensaje de error del backend (múltiples formatos posibles)
          let errorMsg = 'Error al crear la sucursal';

          if (err.error) {
            // Formato RFC 7807 (Problem Details): error.error.detail o error.error.title
            if (err.error.detail && typeof err.error.detail === 'string') {
              errorMsg = err.error.detail;
            } else if (
              err.error.title &&
              typeof err.error.title === 'string' &&
              err.error.title !== 'Bad Request'
            ) {
              errorMsg = err.error.title;
            }
            // Formato 1: error.error.message (Spring Boot estándar)
            else if (
              err.error.message &&
              typeof err.error.message === 'string'
            ) {
              errorMsg = err.error.message;
            }
            // Formato 2: error.error es un string
            else if (typeof err.error === 'string') {
              errorMsg = err.error;
            }
            // Formato 3: error.error.error
            else if (err.error.error && typeof err.error.error === 'string') {
              errorMsg = err.error.error;
            }
            // Formato 4: error.error con status y message
            else if (err.error.status === 'error' && err.error.message) {
              errorMsg = err.error.message;
            }
            // Formato 5: error.error.errors (array de validación)
            else if (
              Array.isArray(err.error.errors) &&
              err.error.errors.length > 0
            ) {
              const firstError = err.error.errors[0];
              errorMsg =
                firstError.message ||
                firstError.defaultMessage ||
                String(firstError);
            }
          }

          // Si el mensaje sigue siendo genérico, intentar obtener más información
          if (errorMsg === 'Error al crear la sucursal' && err.error) {
            // Intentar obtener cualquier campo que contenga "message", "detail" o "title"
            const errorObj = err.error;
            for (const key in errorObj) {
              const lowerKey = key.toLowerCase();
              if (
                (lowerKey.includes('message') ||
                  lowerKey.includes('detail') ||
                  lowerKey.includes('title')) &&
                typeof errorObj[key] === 'string' &&
                errorObj[key].trim() !== '' &&
                errorObj[key] !== 'Bad Request'
              ) {
                errorMsg = errorObj[key];
                break;
              }
            }
          }

          this.logger.error(
            `[SUCURSAL] Error detallado - Mensaje final:`,
            errorMsg
          );

          this.toast.httpError(err, 'crear la sucursal');
        },
      });
    }
  }

  /**
   * Función de comparación para el mat-select de compañías
   * Compara por idCompany (número)
   */
  compareCompanies(
    companyId1: number | null,
    companyId2: number | null
  ): boolean {
    return companyId1 === companyId2;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
