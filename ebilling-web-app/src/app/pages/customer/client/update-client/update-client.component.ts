import {
  Component,
  OnInit,
  Inject,
  OnDestroy,
  ViewEncapsulation,
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Client } from '../../../../_model/customer/client';
import { ClientService } from '../../../../_service/customer/client.service';
import { catchError, EMPTY, of, switchMap, takeUntil, tap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Contact } from '../../../../_model/customer/contact';
import {
  CLIENT_TYPE,
  DEFAULT_CLIENT_TYPE_CODE,
  CONTACT_TYPE_ADRESS,
  CONTACT_TYPE_MAIL,
  CONTACT_TYPE_MOBIL,
  CONTACT_TYPE_PHONE,
  EXIST_DATA,
  HEADER_MESSAGE,
  IDENTIFICATION_TYPE,
  IDENTIFICATION_TYPE_CEDULA_LENGTH,
  IDENTIFICATION_TYPE_DEFAULT_LENGTH,
  IDENTIFICATION_TYPE_PASSPORT_LENGTH,
  IDENTIFICATION_TYPE_RUC_LENGTH,
  ID_TYPE_CEDULA,
  ID_TYPE_RUC,
  ID_TYPE_PASAPORTE,
  ID_TYPE_CONSUMIDOR_FINAL,
  ID_TYPE_EXTERIOR,
  CONSUMIDOR_FINAL_VALUE,
  CONSUMIDOR_FINAL_NAME,
  SUCCESS,
  UPDATE,
} from '../../../../_constants/constants';
import { environment } from 'src/environments/environment';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { ClientHelperService } from '../../../../_service/customer/client-helper.service';
import { ClientSaveDto } from '../../../../_model/customer/client-save-dto';
import { ToastService } from '../../../../_service/notification/toast.service';

export interface ClientDialogData {
  idClient?: number;
  mode: 'create' | 'edit';
  hasConsumidorFinal?: boolean; // true si ya existe un consumidor final activo
  companyEmail?: string; // Email de la compañía para pre-llenar
  initialIdentification?: string; // Identificación inicial para pre-llenar (desde factura)
  initialIdType?: number; // Tipo de identificación inicial para pre-llenar (desde factura)
}

/**
 * Validador personalizado para nombre y apellido
 * Requiere al menos 2 palabras, cada una con mínimo 2 letras
 */
function fullNameValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) {
    return null; // Si está vacío, lo maneja el Validators.required
  }

  const value = control.value.trim();

  // Regex: al menos 2 palabras, cada una con mínimo 2 letras
  // ^: inicio de línea
  // [a-záéíóúñüA-ZÁÉÍÓÚÑÜ]: letras (incluye acentos y ñ)
  // {2,}: al menos 2 caracteres
  // \s+: uno o más espacios
  // [a-záéíóúñüA-ZÁÉÍÓÚÑÜ]{2,}: segunda palabra con al menos 2 letras
  // .*: opcionalmente más palabras
  // $: fin de línea
  const fullNamePattern =
    /^[a-záéíóúñüA-ZÁÉÍÓÚÑÜ]{2,}(\s+[a-záéíóúñüA-ZÁÉÍÓÚÑÜ]{2,})+$/;

  if (!fullNamePattern.test(value)) {
    return {
      fullName: {
        message:
          'Debe ingresar al menos nombre y apellido (mínimo 2 letras cada uno)',
      },
    };
  }

  return null;
}

@Component({
  selector: 'app-update-client',
  templateUrl: './update-client.component.html',
  styleUrls: ['./update-client.component.css'],
})
export class UpdateClientComponent implements OnInit, OnDestroy {
  idClient: number = 0;
  client: Client = {} as Client;
  phone: Contact = {} as Contact;
  mobil: Contact = {} as Contact;
  mail: Contact = {} as Contact;
  address: Contact = {} as Contact;

  form: FormGroup;
  edition: boolean = false;
  clientTypes = CLIENT_TYPE;
  idTypes = IDENTIFICATION_TYPE;
  identificationTypes = IDENTIFICATION_TYPE;

  // ✅ Lista filtrada de tipos de identificación (excluye Consumidor Final si ya existe)
  filteredIdTypes: { code: number; value: string }[] = [];

  idType: number = 0;
  clientType: number = 0;
  identificationMaxlength: number = IDENTIFICATION_TYPE_CEDULA_LENGTH.value;
  identificationMinlength: number = IDENTIFICATION_TYPE_CEDULA_LENGTH.value;
  identificationLast: string = '';

  private destroy$ = new Subject<void>();

  constructor(
    private readonly clientService: ClientService,
    private readonly logger: LoggerService,
    public dialogRef: MatDialogRef<UpdateClientComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ClientDialogData,
    private readonly clientHelper: ClientHelperService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    this.logger.debug('[UpdateClientComponent] Data recibida:', {
      mode: this.data.mode,
      hasConsumidorFinal: this.data.hasConsumidorFinal,
    });

    this.client = {} as Client;

    // ✅ Filtrar tipos de identificación: excluir Consumidor Final si ya existe
    this.filteredIdTypes = this.data.hasConsumidorFinal
      ? IDENTIFICATION_TYPE.filter((t) => t.code !== ID_TYPE_CONSUMIDOR_FINAL)
      : [...IDENTIFICATION_TYPE];

    this.form = new FormGroup({
      id: new FormControl(0),
      idType: new FormControl('', Validators.required),
      identification: new FormControl('', Validators.required),
      name: new FormControl('', [Validators.required, fullNameValidator]),
      phone: new FormControl('', [
        Validators.minLength(9),
        Validators.maxLength(10),
        Validators.pattern('^[0-9]*$'),
      ]),
      mobile: new FormControl('', [
        Validators.minLength(10),
        Validators.maxLength(10),
        Validators.pattern('^[0-9]*$'),
      ]),
      mail: new FormControl('', [Validators.required, Validators.email]),
      address: new FormControl(''),
      status: new FormControl(true),
    });

    // ✅ Subscribe to identification type changes to update validations dynamically
    this.form
      .get('idType')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((idType: number) => {
        this.updateIdentificationValidation(idType);
        this.updateConsumidorFinalFields(idType);
      });

    // Initialize based on dialog data
    if (this.data.mode === 'edit' && this.data.idClient) {
      this.idClient = this.data.idClient;
      this.edition = true;
      this.initForm();
    } else {
      this.edition = false;
      // ✅ Pre-llenar campos si vienen desde factura
      const initialIdType = this.data.initialIdType || ID_TYPE_CEDULA;
      const initialIdentification = this.data.initialIdentification || '';

      // Set default idType and clientType for new clients
      this.idType = initialIdType;
      this.clientType = DEFAULT_CLIENT_TYPE_CODE; // Cliente por defecto (constante)

      // ✅ Primero establecer el tipo de identificación y sus validaciones
      if (this.data.initialIdType) {
        this.updateIdentificationValidation(this.data.initialIdType);
      }

      // Luego pre-llenar valores
      this.form.patchValue({ idType: initialIdType });

      // Pre-llenar identificación si viene desde factura (después de actualizar validaciones)
      if (initialIdentification) {
        this.form.patchValue({ identification: initialIdentification });
      }

      // Actualizar campos de Consumidor Final si aplica (después de establecer valores)
      if (this.data.initialIdType) {
        this.updateConsumidorFinalFields(this.data.initialIdType);
      }

      // ✅ Actualizar el estado de validación del formulario después de pre-llenar
      // Esto asegura que el formulario reconozca el estado válido/inválido
      // Usar setTimeout para asegurar que Angular procese los cambios del formulario
      setTimeout(() => {
        this.form.get('idType')?.updateValueAndValidity({ emitEvent: true });
        this.form
          .get('identification')
          ?.updateValueAndValidity({ emitEvent: true });

        // Solo actualizar name y mail si no son Consumidor Final
        if (this.data.initialIdType !== ID_TYPE_CONSUMIDOR_FINAL) {
          this.form.get('name')?.updateValueAndValidity({ emitEvent: true });
          this.form.get('mail')?.updateValueAndValidity({ emitEvent: true });
        }

        // Forzar actualización del estado del formulario completo
        this.form.updateValueAndValidity({ emitEvent: true });
      }, 0);
    }
  }

  /**
   * ✅ Actualiza campos automáticamente cuando se selecciona Consumidor Final
   */
  private updateConsumidorFinalFields(idType: number): void {
    const nameControl = this.form.get('name');
    const mailControl = this.form.get('mail');

    this.logger.debug(
      '🔄 updateConsumidorFinalFields llamado con idType:',
      idType
    );
    this.logger.debug('📧 companyEmail disponible:', this.data.companyEmail);

    if (idType === ID_TYPE_CONSUMIDOR_FINAL) {
      // ✅ Auto-completar nombre con valor fijo y deshabilitar
      nameControl?.setValue(CONSUMIDOR_FINAL_NAME);
      nameControl?.disable();
      nameControl?.clearValidators(); // No requiere validación fullName

      // ✅ Pre-llenar email con el de la compañía (SIEMPRE si existe)
      if (this.data.companyEmail) {
        this.logger.debug(
          '📧 Aplicando email de compañía:',
          this.data.companyEmail
        );
        mailControl?.setValue(this.data.companyEmail);
      } else {
        this.logger.warn('📧 No hay email de compañía disponible');
      }

      mailControl?.clearValidators();
      mailControl?.setValidators([Validators.email]); // Solo validar formato si tiene valor

      this.logger.debug(
        '✅ Consumidor Final seleccionado - campos auto-completados'
      );
    } else {
      // ✅ Restaurar validaciones normales
      nameControl?.enable();
      nameControl?.setValidators([Validators.required, fullNameValidator]);

      // ✅ Limpiar nombre si venía de Consumidor Final
      if (nameControl?.value === CONSUMIDOR_FINAL_NAME) {
        nameControl?.setValue('');
      }

      // ✅ Limpiar email si venía de Consumidor Final y era el de la compañía
      if (
        this.data.companyEmail &&
        mailControl?.value === this.data.companyEmail
      ) {
        mailControl?.setValue('');
      }

      mailControl?.setValidators([Validators.required, Validators.email]);
    }

    nameControl?.updateValueAndValidity();
    mailControl?.updateValueAndValidity();
  }
  initForm() {
    if (this.edition) {
      this.clientService
        .findById(this.idClient)
        .pipe(
          takeUntil(this.destroy$),
          catchError((error) => {
            this.logger.error('Error loading client data', error);
            this.showValidationSnackbar(
              'Error al cargar los datos del cliente',
              'error'
            );
            return EMPTY;
          })
        )
        .subscribe((data) => {
          if (data.code == 0 && data.clientDtos.length > 0) {
            let client = data.clientDtos[0];

            // Store the complete client object to preserve idCompany and other fields
            this.client = { ...client };
            this.logger.info(
              'Client loaded with idCompany:',
              this.client.idCompany
            );

            this.phone = this.findContact(
              client.contacts,
              CONTACT_TYPE_PHONE.code
            );
            this.mobil = this.findContact(
              client.contacts,
              CONTACT_TYPE_MOBIL.code
            );
            this.address = this.findContact(
              client.contacts,
              CONTACT_TYPE_ADRESS.code
            );
            this.mail = this.findContact(
              client.contacts,
              CONTACT_TYPE_MAIL.code
            );

            // Log contacts loaded with their IDs
            this.logger.info('Contacts loaded from backend:', {
              phone: this.phone,
              mobil: this.mobil,
              address: this.address,
              mail: this.mail,
            });

            this.idType = client.idType;
            this.obtainIdentificationLength();
            this.clientType = client.clientType;
            this.identificationLast = client.identification;
            this.form.patchValue({
              id: client.idClient,
              idType: client.idType || '',
              identification: client.identification,
              name: client.name,
              phone: this.phone.value || '',
              mobile: this.mobil.value || '',
              mail: this.mail.value || '',
              address: this.address.value || '',
              status: client.status,
            });

            // ✅ Aplicar validaciones según el tipo de identificación cargado
            this.updateIdentificationValidation(client.idType);
          }
        });
    }
  }

  findContact(contacts: Contact[], contactType: string): Contact {
    let contact = contacts.find((c) => c.contactType === contactType);
    if (contact === undefined) {
      return {} as Contact;
    } else {
      return contact;
    }
  }

  /**
   * Retorna el placeholder apropiado según el tipo de identificación
   */
  getIdentificationPlaceholder(): string {
    const idType = this.form.get('idType')?.value;
    switch (idType) {
      case ID_TYPE_CEDULA:
        return 'Ej: 1712345678 (10 dígitos)';
      case ID_TYPE_RUC:
        return 'Ej: 1712345678001 (13 dígitos)';
      case ID_TYPE_PASAPORTE:
        return 'Ej: AB123456 (alfanumérico)';
      case ID_TYPE_CONSUMIDOR_FINAL:
        return 'Consumidor final (automático)';
      case ID_TYPE_EXTERIOR:
        return 'Ej: EIN 35-2684422 (máx 20 caracteres)';
      default:
        return 'Número de identificación';
    }
  }

  /**
   * Actualiza las validaciones del campo identificación según el tipo seleccionado
   */
  updateIdentificationValidation(idType: number): void {
    const identificationControl = this.form.get('identification');
    if (!identificationControl) return;

    // ✅ Limpiar el campo si viene de Consumidor Final y cambió a otro tipo
    const currentValue = identificationControl.value;
    if (
      currentValue === CONSUMIDOR_FINAL_VALUE &&
      idType !== ID_TYPE_CONSUMIDOR_FINAL
    ) {
      identificationControl.setValue(''); // Limpiar campo al cambiar desde Consumidor Final
    }

    // Limpiar validaciones previas
    identificationControl.clearValidators();

    switch (idType) {
      case ID_TYPE_CEDULA: // Cédula: 10 dígitos numéricos
        identificationControl.setValidators([
          Validators.required,
          Validators.minLength(10),
          Validators.maxLength(10),
          Validators.pattern('^[0-9]{10}$'), // Solo números, exactamente 10
        ]);
        identificationControl.enable();
        break;

      case ID_TYPE_RUC: // RUC: 13 dígitos numéricos
        identificationControl.setValidators([
          Validators.required,
          Validators.minLength(13),
          Validators.maxLength(13),
          Validators.pattern('^[0-9]{13}$'), // Solo números, exactamente 13
        ]);
        identificationControl.enable();
        break;

      case ID_TYPE_PASAPORTE: // Pasaporte: Alfanumérico, máximo 10 caracteres
        identificationControl.setValidators([
          Validators.required,
          Validators.maxLength(10),
          Validators.pattern('^[A-Za-z0-9]{1,10}$'), // Solo letras y números, máx 10
        ]);
        identificationControl.enable();
        break;

      case ID_TYPE_CONSUMIDOR_FINAL: // Consumidor Final: Valor fijo "9999999999999"
        identificationControl.setValue(CONSUMIDOR_FINAL_VALUE);
        identificationControl.setValidators([Validators.required]);
        identificationControl.disable(); // Campo readonly
        break;

      case ID_TYPE_EXTERIOR: // Identificación del Exterior: formato libre, hasta 20 caracteres
        identificationControl.setValidators([
          Validators.required,
          Validators.maxLength(20),
          Validators.pattern('^[A-Za-z0-9\\s\\-\\.]{1,20}$'),
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

  /**
   * Muestra un toast de validación usando ToastService
   */
  private showValidationSnackbar(
    message: string,
    type: 'error' | 'warning' | 'info' | 'success' = 'error'
  ): void {
    this.logger.info('🎯 showValidationSnackbar() llamado:', {
      message,
      type,
    });

    switch (type) {
      case 'success':
        this.toast.success(message);
        break;
      case 'warning':
        this.toast.warning(message);
        break;
      case 'info':
        this.toast.info(message);
        break;
      case 'error':
      default:
        this.toast.error(message);
        break;
    }
  }

  /**
   * Procesa la respuesta del backend para detectar warnings o mensajes especiales
   */
  validateSave() {
    // Validate form before saving
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.showValidationSnackbar(
        'Por favor, corrija los errores en el formulario antes de guardar.',
        'warning'
      );
      return;
    }

    // ✅ Simplificado: El backend maneja duplicados, actualización y reactivación
    // Ya no validamos con endpoint "exist" que fue eliminado
    this.save();
  }

  save() {
    let companyIdentification: string;
    try {
      companyIdentification =
        this.clientHelper.getCompanyIdentificationFromSession(
          environment.IDENTIFICATION
        );
    } catch (error) {
      this.logger.error('Error getting company identification', error);
      this.showValidationSnackbar(
        'Sesión expirada. Por favor inicie sesión nuevamente.',
        'error'
      );
      return;
    }

    // Build client object with only required fields
    // ✅ Usar getRawValue() para incluir campos disabled (ej: Consumidor Final)
    const formValues = this.form.getRawValue();
    const clientToSend: ClientSaveDto = {
      idType: formValues['idType'] || this.idType,
      clientType: this.clientType,
      identification: formValues['identification'], // ✅ Incluye valor aunque esté disabled
      name: formValues['name'],
      status: formValues['status'],
      companyIdentification: companyIdentification,
      contacts: this.addContacts(),
    };

    // Only include idClient if it's an existing client (> 0)
    // For new clients, DO NOT include idClient at all
    if (this.idClient && this.idClient > 0) {
      clientToSend.idClient = this.idClient;
    }

    // Only include idCompany if it has a valid value from backend
    // Do NOT include it at all if undefined or null to avoid sending null
    if (this.client.idCompany != null && this.client.idCompany > 0) {
      clientToSend.idCompany = this.client.idCompany;
    }

    // Remove undefined AND null fields to avoid backend errors
    (Object.keys(clientToSend) as Array<keyof ClientSaveDto>).forEach((key) => {
      if (clientToSend[key] === undefined || clientToSend[key] === null) {
        delete clientToSend[key];
      }
    });

    // Update client reference (cast to Client for compatibility)
    this.client = clientToSend as unknown as Client;

    // Log data being sent for debugging
    this.logger.info('===== CLIENT DATA TO BE SENT =====');
    this.logger.info(
      'Client data being sent:',
      JSON.stringify(this.client, null, 2)
    );
    this.logger.info('Has idCompany field?', 'idCompany' in this.client);
    this.logger.info('idCompany value:', this.client.idCompany);
    this.logger.info('Object keys:', Object.keys(this.client));
    this.logger.info('Number of contacts:', this.client.contacts?.length);
    this.logger.info('idType:', this.client.idType);
    this.logger.info('clientType:', this.client.clientType);
    this.logger.info('identification:', this.client.identification);
    this.logger.info(
      'identification length:',
      this.client.identification?.length
    );
    this.logger.info('===================================');

    if (this.client.idClient > 0) {
      // Update existing client
      this.clientService
        .update(this.client, this.client.idClient)
        .pipe(
          takeUntil(this.destroy$),
          catchError((err: HttpErrorResponse) => {
            this.logger.error(
              '❌ Error durante la actualización del cliente:',
              err
            );

            // ✅ HTTP 4xx = ERROR: Mostrar mensaje, NO cerrar modal, NO actualizar lista
            if (err.status >= 400 && err.status < 500) {
              // Extraer mensaje del backend con prioridad correcta
              let backendMessage = 'Ocurrió un error al actualizar el cliente.';

              // 1️⃣ PRIORIDAD: Verificar array de errores (validaciones de Spring Boot)
              if (err.error?.errors && Array.isArray(err.error.errors)) {
                backendMessage = err.error.errors
                  .map(
                    (e: any) => e.message || e.defaultMessage || e.field || e
                  )
                  .filter((msg: string) => msg)
                  .join(', ');
              }
              // 2️⃣ Verificar fieldErrors (formato alternativo)
              else if (
                err.error?.fieldErrors &&
                Array.isArray(err.error.fieldErrors)
              ) {
                backendMessage = err.error.fieldErrors
                  .map(
                    (e: any) =>
                      e.message ||
                      e.defaultMessage ||
                      `${e.field}: ${e.message}`
                  )
                  .filter((msg: string) => msg)
                  .join(', ');
              }
              // 3️⃣ Mensaje directo (puede ser verbose de Spring Boot)
              else if (err.error?.message) {
                backendMessage = err.error.message;

                // Extraer mensaje limpio del formato verbose de Spring Boot
                // Buscar TODOS los "default message [...]" y tomar el ÚLTIMO
                const allMatches = backendMessage.match(
                  /default message \[([^\]]+)\]/g
                );

                if (allMatches && allMatches.length > 0) {
                  // Tomar el ÚLTIMO match (el más relevante)
                  const lastMatch = allMatches[allMatches.length - 1];
                  const extracted = lastMatch.match(
                    /default message \[([^\]]+)\]/
                  );
                  if (extracted && extracted[1]) {
                    backendMessage = extracted[1];
                  }
                }
              }
              // 4️⃣ Error como string simple
              else if (typeof err.error === 'string') {
                backendMessage = err.error;
              }

              this.logger.error('🔴 HTTP 4xx detectado:', {
                status: err.status,
                message: backendMessage,
              });

              this.showValidationSnackbar(backendMessage, 'error');

              // ❌ NO continuar al subscribe (NO cerrar modal, NO actualizar lista)
              return EMPTY;
            }

            // Otros errores (5xx, network, etc.)
            this.showValidationSnackbar(
              'Ocurrió un error al actualizar el cliente.',
              'error'
            );
            return EMPTY;
          }),
          switchMap(() => {
            // ✅ Solo llega aquí si fue HTTP 2xx (éxito)
            // Refrescar la lista de clientes
            return this.clientService.findByCompany(companyIdentification).pipe(
              catchError((refreshErr) => {
                this.logger.warn(
                  'Cliente actualizado correctamente, pero falló al refrescar la lista',
                  refreshErr
                );
                return of({ clientDtos: [] });
              })
            );
          })
        )
        .subscribe((data) => {
          // ✅ Solo llega aquí si fue HTTP 2xx (UPDATE exitoso)
          this.logger.info('✅ Cliente actualizado exitosamente');

          if (data.clientDtos && data.clientDtos.length > 0) {
            let clientList = data.clientDtos;
            this.clientHelper.addContactInformation(clientList);
            this.clientService.clientsChange.next(clientList);
          }

          // ✅ Mostrar toast personalizado de éxito
          this.showValidationSnackbar(
            UPDATE.MESSAGE_UPDATE_CLIENT.message,
            'success'
          );

          // ✅ Cerrar el modal
          this.dialogRef.close(true);
        });
    } else {
      // Create new client
      this.clientService
        .save(this.client)
        .pipe(
          takeUntil(this.destroy$),
          catchError((err: HttpErrorResponse) => {
            this.logger.error('❌ Error durante el guardado del cliente:', err);

            // ✅ HTTP 4xx = ERROR: Mostrar mensaje, NO cerrar modal, NO actualizar lista
            if (err.status >= 400 && err.status < 500) {
              // Extraer mensaje del backend con prioridad correcta
              let backendMessage =
                'Datos inválidos. Por favor revise los campos.';

              // 1️⃣ PRIORIDAD: Verificar array de errores (validaciones de Spring Boot)
              if (err.error?.errors && Array.isArray(err.error.errors)) {
                backendMessage = err.error.errors
                  .map(
                    (e: any) => e.message || e.defaultMessage || e.field || e
                  )
                  .filter((msg: string) => msg)
                  .join(', ');
              }
              // 2️⃣ Verificar fieldErrors (formato alternativo)
              else if (
                err.error?.fieldErrors &&
                Array.isArray(err.error.fieldErrors)
              ) {
                backendMessage = err.error.fieldErrors
                  .map(
                    (e: any) =>
                      e.message ||
                      e.defaultMessage ||
                      `${e.field}: ${e.message}`
                  )
                  .filter((msg: string) => msg)
                  .join(', ');
              }
              // 3️⃣ Mensaje directo (puede ser verbose de Spring Boot)
              else if (err.error?.message) {
                backendMessage = err.error.message;

                // Extraer mensaje limpio del formato verbose de Spring Boot
                // Buscar TODOS los "default message [...]" y tomar el ÚLTIMO
                const allMatches = backendMessage.match(
                  /default message \[([^\]]+)\]/g
                );

                if (allMatches && allMatches.length > 0) {
                  // Tomar el ÚLTIMO match (el más relevante)
                  const lastMatch = allMatches[allMatches.length - 1];

                  const extracted = lastMatch.match(
                    /default message \[([^\]]+)\]/
                  );
                  if (extracted && extracted[1]) {
                    backendMessage = extracted[1];
                  }
                }
              }
              // 4️⃣ Error como string simple
              else if (typeof err.error === 'string') {
                backendMessage = err.error;
              }

              this.logger.error('🔴 HTTP 4xx detectado:', {
                status: err.status,
                message: backendMessage,
              });

              this.showValidationSnackbar(backendMessage, 'error');

              // ❌ NO continuar al subscribe (NO cerrar modal, NO actualizar lista)
              return EMPTY;
            }

            // Otros errores (5xx, network, etc.)
            this.showValidationSnackbar(
              'Ocurrió un error al guardar el cliente.',
              'error'
            );
            return EMPTY;
          }),
          switchMap(() => {
            // ✅ Solo llega aquí si fue HTTP 2xx (éxito)
            // Refrescar la lista de clientes
            return this.clientService.findByCompany(companyIdentification).pipe(
              catchError((refreshErr) => {
                this.logger.warn(
                  'Cliente guardado correctamente, pero falló al refrescar la lista',
                  refreshErr
                );
                return of({ clientDtos: [] });
              })
            );
          })
        )
        .subscribe((data) => {
          // ✅ Solo llega aquí si fue HTTP 2xx (CREATE exitoso o reactivado)
          this.logger.info('✅ Cliente guardado/reactivado exitosamente');

          if (data.clientDtos && data.clientDtos.length > 0) {
            let clientList = data.clientDtos;
            this.clientHelper.addContactInformation(clientList);
            this.clientService.clientsChange.next(clientList);

            // ✅ Buscar el cliente recién creado por identificación para retornarlo
            const savedClient = clientList.find(
              (c) => c.identification === this.client.identification
            );
            if (savedClient) {
              // ✅ Cerrar el modal retornando el cliente creado
              this.dialogRef.close(savedClient);
            } else {
              // Si no se encuentra, cerrar con true (comportamiento anterior)
              this.dialogRef.close(true);
            }
          } else {
            // ✅ Cerrar el modal retornando el cliente construido desde el formulario
            // Construir el objeto Client completo con los datos del formulario
            const createdClient: Client = {
              ...this.client,
              contacts: this.addContacts(),
            };
            this.clientHelper.addContactInformation([createdClient]);
            this.dialogRef.close(createdClient);
          }

          // ✅ Mostrar toast personalizado de éxito
          this.showValidationSnackbar(
            SUCCESS.MESSAGE_REGISTER_CLIENT.message,
            'success'
          );
        });
    }
  }

  addContacts(): Contact[] {
    let contacts: Contact[] = [];
    let phone = this.form.value['phone'];
    let mobil = this.form.value['mobile'];
    let address = this.form.value['address'];
    let email = this.form.value['mail'];

    if (phone && phone.trim() !== '') {
      const phoneContact: Partial<Contact> = {
        contactType: CONTACT_TYPE_PHONE.code,
        value: phone.trim(),
        status: true,
      };
      // Only include idContact if it's an existing contact (> 0)
      if (this.phone?.idContact && this.phone.idContact > 0) {
        phoneContact.idContact = this.phone.idContact;
      }
      contacts.push(phoneContact as Contact);
    }

    if (address && address.trim() !== '') {
      const addressContact: Partial<Contact> = {
        contactType: CONTACT_TYPE_ADRESS.code,
        value: address.trim(),
        status: true,
      };
      // Only include idContact if it's an existing contact (> 0)
      if (this.address?.idContact && this.address.idContact > 0) {
        addressContact.idContact = this.address.idContact;
      }
      contacts.push(addressContact as Contact);
    }

    if (mobil && mobil.trim() !== '') {
      const mobilContact: Partial<Contact> = {
        contactType: CONTACT_TYPE_MOBIL.code,
        value: mobil.trim(),
        status: true,
      };
      // Only include idContact if it's an existing contact (> 0)
      if (this.mobil?.idContact && this.mobil.idContact > 0) {
        mobilContact.idContact = this.mobil.idContact;
      }
      contacts.push(mobilContact as Contact);
    }

    if (email && email.trim() !== '') {
      const emailContact: Partial<Contact> = {
        contactType: CONTACT_TYPE_MAIL.code,
        value: email.trim(),
        status: true,
      };
      // Only include idContact if it's an existing contact (> 0)
      if (this.mail?.idContact && this.mail.idContact > 0) {
        emailContact.idContact = this.mail.idContact;
      }
      contacts.push(emailContact as Contact);
    }

    this.logger.info('Contacts being sent:', contacts);
    // Detailed log for each contact
    contacts.forEach((contact, index) => {
      this.logger.info(`Contact ${index + 1}:`, {
        hasIdContact: 'idContact' in contact,
        idContact: contact.idContact,
        contactType: contact.contactType,
        value: contact.value,
        status: contact.status,
      });
    });
    return contacts;
  }

  get f() {
    return this.form.controls;
  }

  clearIdentification() {
    this.obtainIdentificationLength();
    this.form.get('identification')?.reset();
  }

  obtainIdentificationLength() {
    if (this.idType === 1) {
      this.identificationMaxlength = IDENTIFICATION_TYPE_RUC_LENGTH.value;
      this.identificationMinlength = IDENTIFICATION_TYPE_RUC_LENGTH.value;
    }
    if (this.idType === 2) {
      this.identificationMaxlength = IDENTIFICATION_TYPE_CEDULA_LENGTH.value;
      this.identificationMinlength = IDENTIFICATION_TYPE_CEDULA_LENGTH.value;
    }
    if (this.idType === 3) {
      this.identificationMaxlength = IDENTIFICATION_TYPE_PASSPORT_LENGTH.value;
      this.identificationMinlength = IDENTIFICATION_TYPE_DEFAULT_LENGTH.value;
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
