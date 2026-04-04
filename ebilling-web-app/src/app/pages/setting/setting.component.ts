import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import {
  DURATION_TIME_MESSAGE,
  EMPTY_DATA,
} from 'src/app/_constants/constants';
import { DigitalSignatureService } from 'src/app/_service/customer/digital-signature.service';
import { environment } from 'src/environments/environment';
import { DigitalSignature } from '../../_model/customer/digital-signature';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { LoggerService } from '../../_service/logger/logger.service';
import { ToastService } from '../../_service/notification/toast.service';
import { decodeToken } from '../../_functions/functions';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-setting',
  templateUrl: './setting.component.html',
  styleUrls: ['./setting.component.css'],
})
export class SettingComponent implements OnInit {
  certificateFileName: string | null = null;
  selectedCertificateFile: File | null = null;
  fechaVigenciaDisplay: string = '';
  propietarioCertificadoDisplay: string = '';
  idDigitalSignature: number = 0;
  digitalSignature: DigitalSignature;
  isActive: boolean = false;
  validatedPassword: string = ''; // Password que fue validado exitosamente

  form: FormGroup;

  constructor(
    private http: HttpClient,
    private readonly digitalSignatureService: DigitalSignatureService,
    private readonly logger: LoggerService,
    private readonly toast: ToastService
  ) {
    this.form = new FormGroup({
      id: new FormControl(0),
      passwordCertificate: new FormControl(''),
    });
    this.digitalSignature = this.createEmptyDigitalSignature(); // Inicializa el objeto de firma digital
  } // Inyecta HttpClient
  ngOnInit(): void {
    // Verificar que el usuario esté autenticado y tenga un idCompany válido
    const token = decodeToken();
    const idCompanyRaw = localStorage.getItem(environment.ID_COMPANY);
    const idCompany = idCompanyRaw ? Number(idCompanyRaw) : 0;

    // Solo hacer la llamada si hay token válido y idCompany válido (mayor a 0)
    if (!token || !idCompany || idCompany <= 0) {
      this.logger.debug(
        '[SettingComponent] Usuario no autenticado o sin compañía, omitiendo carga de firma digital',
        {
          hasToken: !!token,
          idCompany: idCompany,
        }
      );
      // Inicializar con valores vacíos si no hay autenticación
      this.createEmptyDigitalSignature();
      this.selectedCertificateFile = null;
      return;
    }

    this.digitalSignatureService
      .findByIdcompany(idCompany)
      .pipe(
        catchError((error) => {
          // Manejar errores 401 (no autenticado) o 404 (no encontrado) de forma silenciosa
          if (error.status === 401 || error.status === 404) {
            this.logger.debug(
              '[SettingComponent] No se pudo cargar la firma digital (usuario no autenticado o no existe)',
              {
                status: error.status,
                idCompany: idCompany,
              }
            );
            // Inicializar con valores vacíos
            this.createEmptyDigitalSignature();
            this.selectedCertificateFile = null;
            return of({
              code: error.status,
              status: 'error',
              message: error.message,
              digitalSignatureDto: null,
              digitalSignatureDtos: [],
            });
          }
          // Para otros errores, loggear y retornar respuesta vacía
          this.logger.error(
            '[SettingComponent] Error al cargar firma digital',
            error
          );
          return of({
            code: error.status || 500,
            status: 'error',
            message: error.message,
            digitalSignatureDto: null,
            digitalSignatureDtos: [],
          });
        })
      )
      .subscribe((data) => {
        if (
          data.digitalSignatureDto != null &&
          data.digitalSignatureDto.signFile
        ) {
          this.idDigitalSignature = data.digitalSignatureDto.id || 0;
          this.form = new FormGroup({
            id: new FormControl(this.idDigitalSignature || 0),
            passwordCertificate: new FormControl(
              data.digitalSignatureDto.password
            ),
          });

          this.fechaVigenciaDisplay =
            data.digitalSignatureDto.certificateInfo?.effectiveDate || 'N/A'; // Actualiza la fecha
          this.propietarioCertificadoDisplay =
            data.digitalSignatureDto.certificateInfo?.owner || 'N/A';
          this.certificateFileName = data.digitalSignatureDto.fileName || 'N/A'; // Actualiza el nombre del archivo
          this.digitalSignature = data.digitalSignatureDto; // Guarda el objeto de firma digital
          this.isActive = true; // ✅ Certificado ya guardado y válido - habilita el botón Guardar
          this.validatedPassword = data.digitalSignatureDto.password || ''; // Guardar el password del certificado guardado
        } else {
          this.createEmptyDigitalSignature();
          this.selectedCertificateFile = null;
        }
      });
  }

  private createEmptyDigitalSignature(): DigitalSignature {
    return {
      id: 0,
      fileName: '',
      signFile: '', // Inicializa como null o un Blob vacío
      password: '', // Valor por defecto
      effectiveDate: '', // Valor por defecto
      idCompany: 0, // Valor por defecto
      certificateInfo: {
        effectiveDate: '',
        owner: '',
      },
    };
  }
  onCertificateFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedCertificateFile = file;
      this.certificateFileName = file.name;
      // Limpiar datos solo cuando se selecciona un nuevo archivo
      this.fechaVigenciaDisplay = '';
      this.propietarioCertificadoDisplay = '';
      this.isActive = false; // ❌ Nuevo archivo requiere validación - deshabilita el botón
      this.validatedPassword = ''; // Limpiar password validado
      this.form.setValue({
        id: this.idDigitalSignature,
        passwordCertificate: '',
      });
      // Validar automáticamente si ya hay un password ingresado
      // Si no hay password, esperar a que el usuario lo ingrese
    } else {
      this.certificateFileName = null;
      this.selectedCertificateFile = null;
    }
  }

  uploadCertificate(): void {
    // Validar si hay un archivo nuevo seleccionado
    // O si hay un certificado guardado pero el usuario quiere validar el password
    if (!this.selectedCertificateFile) {
      // Si no hay archivo nuevo y hay certificado guardado, no validar automáticamente
      // El usuario debe seleccionar el archivo para validar
      if (this.idDigitalSignature > 0) {
        this.toast.warning(
          'Para validar el password, debe seleccionar el archivo del certificado.'
        );
      }
      return;
    }

    // Validar password solo si hay un archivo nuevo
    if (
      this.form.value['passwordCertificate'] === undefined ||
      this.form.value['passwordCertificate'] === '' ||
      this.form.value['passwordCertificate'] === null
    ) {
      // Solo limpiar datos si hay un archivo nuevo pero sin password
      this.fechaVigenciaDisplay = '';
      this.propietarioCertificadoDisplay = '';
      this.toast.warning('Por favor, ingrese la contraseña del certificado.');
      return;
    }

    if (this.selectedCertificateFile) {
      const formData = new FormData();
      formData.append(
        'file',
        this.selectedCertificateFile,
        this.selectedCertificateFile.name
      );

      this.digitalSignatureService
        .uploadFile(
          this.selectedCertificateFile,
          this.form.value['passwordCertificate']
        )
        .subscribe({
          next: (data) => {
            if (data.code === 200) {
              this.fechaVigenciaDisplay =
                data.certificateInfoDto?.effectiveDate || 'N/A'; // Actualiza la fecha
              this.propietarioCertificadoDisplay =
                data.certificateInfoDto?.owner || 'N/A'; // Actualiza el propietario
              this.isActive = true; // ✅ Certificado validado correctamente - habilita el botón Guardar
              this.validatedPassword = this.form.value['passwordCertificate']; // Guardar el password validado
              this.toast.success('Certificado validado correctamente');
            } else if (data.code === 404) {
              this.isActive = false; // ❌ Password incorrecto - deshabilita el botón
              this.validatedPassword = ''; // Limpiar password validado
              this.showMessageDigitalSignature(
                'Password incorrecta o certificado inválido.'
              );
            } else {
              this.isActive = false; // ❌ Error en validación - deshabilita el botón
              this.validatedPassword = ''; // Limpiar password validado
              this.showMessageDigitalSignature(
                'Password incorrecta o certificado inválido.'
              );
            }
          },
          error: (error) => {
            this.isActive = false; // ❌ Error en la petición - deshabilita el botón
            this.validatedPassword = ''; // Limpiar password validado
            this.logger.error('Error al validar certificado', error);
            this.showMessageDigitalSignature(
              'Error al validar el certificado. Verifique el password e intente nuevamente.'
            );
          },
        });
    }
  }

  showMessageDigitalSignature(message: string): void {
    // Solo limpiar datos si hay un archivo nuevo seleccionado
    // Si el certificado ya estaba cargado, mantener los datos existentes
    if (this.selectedCertificateFile) {
      this.fechaVigenciaDisplay = '';
      this.propietarioCertificadoDisplay = '';
      this.isActive = false; // ❌ Password incorrecto - deshabilita el botón Guardar
      this.validatedPassword = ''; // Limpiar password validado
      this.form.setValue({
        id: this.idDigitalSignature,
        passwordCertificate: '',
      });
    }
    this.toast.error(message);
  }

  save(): void {
    // Validar que haya password
    if (!this.form.value['passwordCertificate']) {
      this.toast.warning('Por favor, ingrese la contraseña del certificado.');
      return;
    }

    this.digitalSignature.idCompany = Number(
      localStorage.getItem(environment.ID_COMPANY) || '0'
    );

    // Si hay un certificado ya guardado, permitir actualizar solo el password
    if (this.idDigitalSignature > 0) {
      // Si hay un archivo nuevo, actualizar con el archivo
      if (this.selectedCertificateFile) {
        this.digitalSignatureService
          .update(
            this.selectedCertificateFile,
            this.digitalSignature.idCompany,
            this.form.value['passwordCertificate'],
            this.idDigitalSignature
          )
          .subscribe({
            next: (response) => {
              this.logger.info(
                'Firma digital actualizada correctamente',
                response
              );
              this.toast.success('Firma digital actualizada con éxito');
              this.isActive = false;
              // Recargar datos del certificado
              this.ngOnInit();
            },
            error: (error) => {
              this.logger.error('Error al actualizar firma digital', error);
              this.toast.error('Error al actualizar la firma digital');
            },
          });
      } else {
        // Si no hay archivo nuevo pero hay certificado guardado, solo actualizar password
        // Nota: Esto requiere que el backend tenga un endpoint para actualizar solo el password
        // Por ahora, mostrar mensaje informativo
        this.toast.warning(
          'Para actualizar el password, debe seleccionar el archivo del certificado nuevamente.'
        );
      }
    } else if (this.selectedCertificateFile) {
      // Si no hay certificado guardado, crear uno nuevo
      this.digitalSignatureService
        .save(
          this.selectedCertificateFile,
          this.digitalSignature.idCompany,
          this.form.value['passwordCertificate']
        )
        .subscribe({
          next: (response) => {
            this.logger.info('Firma digital guardada correctamente', response);
            this.toast.success('Firma digital guardada con éxito');
            this.isActive = false;
            // Recargar datos del certificado
            this.ngOnInit();
          },
          error: (error) => {
            this.logger.error('Error al guardar firma digital', error);
            this.toast.error('Error al guardar la firma digital');
          },
        });
    } else {
      this.toast.warning('Por favor, seleccione un archivo de certificado.');
    }
  }
}
