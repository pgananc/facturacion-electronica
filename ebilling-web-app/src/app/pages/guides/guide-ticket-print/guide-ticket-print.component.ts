import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { GuideService } from '../../../_service/guide/guide.service';
import { GuideDto, GuideDetailDto, CompanyDto } from '../../../_model/guide/guide-dto';
import { environment } from '../../../../environments/environment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { catchError, map } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-guide-ticket-print',
  templateUrl: './guide-ticket-print.component.html',
  styleUrls: ['./guide-ticket-print.component.css'],
})
export class GuideTicketPrintComponent implements OnInit, AfterViewInit, OnDestroy {
  guide: GuideDto | null = null;
  isLoading = true;
  error: string | null = null;

  // Datos de la empresa desde localStorage
  companyName: string = '';
  companyRuc: string = '';
  companyPhone: string = '';
  companyAddress: string = '';

  // Handler para el evento afterprint (cierre automático después de imprimir)
  private afterPrintHandler?: () => void;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private guideService: GuideService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    const guideId = this.route.snapshot.paramMap.get('id');

    if (!guideId) {
      this.error = 'No se proporcionó ID de guía';
      this.isLoading = false;
      return;
    }

    this.loadCompanyData();
    this.loadGuide(Number(guideId));
  }

  ngAfterViewInit(): void {
    // Solo auto-imprimir si no hay error y la guía está cargada
    setTimeout(() => {
      if (this.error || this.isLoading) {
        return;
      }

      // ✅ Asegurar que el título esté actualizado antes de imprimir
      this.setDocumentTitle();

      // Configurar el listener para cerrar la ventana después de que se cierre el diálogo de impresión
      this.afterPrintHandler = () => {
        setTimeout(() => {
          this.closeWindow();
        }, 500);
      };

      window.addEventListener('afterprint', this.afterPrintHandler);

      // Auto-imprimir cuando la página esté lista
      window.print();
    }, 500);
  }

  ngOnDestroy(): void {
    // Limpiar el listener para evitar memory leaks
    if (this.afterPrintHandler) {
      window.removeEventListener('afterprint', this.afterPrintHandler);
    }
  }

  private loadCompanyData(): void {
    this.companyName =
      localStorage.getItem(environment.NAME_COMPANY) || 'EMPRESA';
    this.companyRuc = localStorage.getItem(environment.IDENTIFICATION) || '';

    const contactsStr = localStorage.getItem('COMPANY_CONTACTS');
    if (contactsStr) {
      try {
        const contacts = JSON.parse(contactsStr);
        const phoneContact = contacts.find(
          (c: any) => c.contactType === 'TEL' && c.status
        );
        const addressContact = contacts.find(
          (c: any) => c.contactType === 'DIR' && c.status
        );

        this.companyPhone = phoneContact?.value || '';
        this.companyAddress = addressContact?.value || '';
      } catch (error) {
        this.logger.warn('Error al parsear COMPANY_CONTACTS', error);
      }
    }
  }

  /**
   * ✅ MÉTODO OPTIMIZADO: Carga la guía desde sessionStorage si existe, sino desde el backend
   */
  private loadGuide(guideId: number): void {
    const storageKey = `GUIDE_TICKET_${guideId}`;
    const storedData = sessionStorage.getItem(storageKey);

    if (storedData) {
      // ✅ DATOS DISPONIBLES EN sessionStorage (caso común: impresión automática)
      try {
        this.guide = JSON.parse(storedData);
        this.completeGuideData();
        this.isLoading = false;

        this.logger.debug('✅ Guía cargada desde sessionStorage', {
          guideId,
          hasGuideNumber: !!this.guide?.guideNumber,
        });
      } catch (error) {
        this.logger.error('Error al parsear datos de sessionStorage', error);
        // Si hay error al parsear, intentar cargar desde el backend
        this.loadGuideFromBackend(guideId);
      }
    } else {
      // ✅ FALLBACK: Cargar desde el backend (caso de recarga de página o acceso directo)
      this.loadGuideFromBackend(guideId);
    }
  }

  /**
   * ✅ MÉTODO: Carga la guía desde el backend (fallback)
   */
  private loadGuideFromBackend(guideId: number): void {
    this.guideService.getGuideById(guideId).subscribe({
      next: (response: any) => {
        // El backend puede retornar GuideDto directamente o dentro de un objeto response
        let guide: GuideDto;
        
        if (response?.code === 200 && response?.data) {
          // Si viene en formato response con code y data
          guide = response.data;
        } else if (response?.code === 200 && response?.guideDto) {
          // Si viene en formato GuideResponse
          guide = response.guideDto;
        } else if (response?.guideNumber || response?.idGuide) {
          // Si es GuideDto directamente
          guide = response;
        } else {
          this.error = 'Formato de respuesta inválido del servidor';
          this.isLoading = false;
          this.cancelAutoPrint();
          return;
        }

        // Validar que la guía tenga los datos mínimos necesarios
        if (!guide) {
          this.error = 'La guía retornada está vacía';
          this.isLoading = false;
          this.cancelAutoPrint();
          return;
        }

        // Si falta companyDto, completarlo desde localStorage
        if (!guide.companyDto) {
          this.logger.warn('⚠️ Guía sin companyDto, completando desde localStorage');
          const companyId = Number(localStorage.getItem(environment.ID_COMPANY)) || 0;
          const companyName = localStorage.getItem(environment.NAME_COMPANY) || 'EMPRESA';
          const companyIdentification = localStorage.getItem(environment.IDENTIFICATION) || '';
          
          guide.companyDto = {
            idCompany: companyId,
            name: companyName,
            identification: companyIdentification,
            nameCommercial: companyName,
          } as CompanyDto;
        }
        
        // Asegurar que companyDto tenga todos los campos requeridos
        if (guide.companyDto && (!guide.companyDto.idCompany || !guide.companyDto.name || !guide.companyDto.identification)) {
          this.logger.warn('⚠️ companyDto incompleto, completando campos faltantes');
          if (!guide.companyDto.idCompany) {
            guide.companyDto.idCompany = Number(localStorage.getItem(environment.ID_COMPANY)) || 0;
          }
          if (!guide.companyDto.name) {
            guide.companyDto.name = localStorage.getItem(environment.NAME_COMPANY) || 'EMPRESA';
          }
          if (!guide.companyDto.identification) {
            guide.companyDto.identification = localStorage.getItem(environment.IDENTIFICATION) || '';
          }
        }

        this.guide = guide;
        this.completeGuideData();
        this.isLoading = false;
        this.logger.debug('✅ Guía cargada desde backend', {
          guideId,
          hasGuideNumber: !!this.guide?.guideNumber,
          hasCompanyDto: !!this.guide?.companyDto,
          responseFormat: response?.code ? 'response' : 'direct',
        });
      },
      error: (err) => {
        this.logger.error('❌ Error al cargar guía para impresión', err);

        // Manejar errores específicos
        if (err.status === 401 || err.status === 403) {
          this.error =
            'No tiene permisos para acceder a esta guía. Por favor, inicie sesión e intente nuevamente.';
        } else if (err.status === 404) {
          this.error = 'La guía solicitada no existe.';
        } else if (err.status === 0) {
          this.error =
            'No se pudo conectar con el servidor. Verifique su conexión e intente nuevamente.';
        } else if (err.status === 500) {
          // Error 500 puede ser por validación del backend (companyDto null)
          // Intentar extraer el mensaje de error
          const errorMessage = err.error?.message || err.error?.error || err.message || 'Error del servidor';
          
          // Si el error menciona companyDto, es un problema del backend
          if (errorMessage.includes('companyDto')) {
            this.error = 'Error en los datos de la guía. La guía no tiene información de la compañía asociada.';
            this.logger.error('Error relacionado con companyDto:', {
              status: err.status,
              message: errorMessage,
              error: err.error,
            });
          } else {
            this.error = `Error al cargar la guía: ${errorMessage}. Por favor, intente nuevamente.`;
          }
        } else {
          // Intentar leer el mensaje de error del backend
          const errorMessage = err.error?.message || err.error?.error || err.message || 'Error desconocido';
          this.error = `Error al cargar la guía: ${errorMessage}. Por favor, intente nuevamente.`;
          this.logger.error('Detalles del error:', {
            status: err.status,
            statusText: err.statusText,
            error: err.error,
          });
        }

        this.isLoading = false;
        this.cancelAutoPrint();
      },
    });
  }

  /**
   * Completa los datos de la guía
   */
  private completeGuideData(): void {
    if (!this.guide) return;

    // Completar dirección si no se encontró en contactos
    if (!this.companyAddress && this.guide.establishment?.address) {
      this.companyAddress = this.guide.establishment.address;
    }

    // Usar nombre de la empresa del guide si está disponible
    if (this.guide.companyDto?.name) {
      this.companyName = this.guide.companyDto.name;
    }

    // ✅ Establecer título de página con identificación del cliente
    this.setDocumentTitle();
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const day = String(date.getDate()).padStart(2, '0');
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const year = date.getFullYear();
      return `${day}/${month}/${year}`;
    } catch (error) {
      return dateStr;
    }
  }

  formatAccessKey(accessKey: string | undefined): string {
    if (!accessKey) return 'N/A';
    if (accessKey.length <= 30) return accessKey;
    return `${accessKey.substring(0, 15)}...${accessKey.substring(
      accessKey.length - 10
    )}`;
  }

  getIdentificationTypeName(idType: number | undefined): string {
    if (!idType) return '';
    const types: { [key: number]: string } = {
      1: 'CEDULA',
      2: 'RUC',
      3: 'PASAPORTE',
      4: 'CONSUMIDOR FINAL',
      5: 'EXTERIOR',
    };
    return types[idType] || 'OTRO';
  }

  getGuideDetails() {
    return this.guide?.guideDetails || [];
  }

  /**
   * Verifica si la guía tiene número (está autorizada)
   */
  hasGuideNumber(): boolean {
    return !!this.guide?.guideNumber;
  }

  /**
   * Establece el título de la página con el número de identificación del cliente/destinatario
   * Esto se usará como nombre de archivo cuando se guarde como PDF
   */
  private setDocumentTitle(): void {
    // En guías, el cliente puede estar en clientDto (destinatario)
    const identification = this.guide?.clientDto?.identification;
    
    if (!identification) {
      // Si no hay identificación, usar el número de guía o ID
      const guideId = this.route.snapshot.paramMap.get('id');
      const fallback = this.guide?.guideNumber || `Guia-${guideId}`;
      document.title = `Ticket-${fallback}`;
      return;
    }

    // Limpiar caracteres especiales que pueden causar problemas en nombres de archivo
    const cleanIdentification = identification.replace(/[^a-zA-Z0-9-]/g, '_');
    
    document.title = `Ticket-${cleanIdentification}`;
  }

  /**
   * Cancela la impresión automática si hay error
   */
  private cancelAutoPrint(): void {
    if (this.afterPrintHandler) {
      window.removeEventListener('afterprint', this.afterPrintHandler);
      this.afterPrintHandler = undefined;
    }
  }

  /**
   * Reimprime el ticket (llamado desde botón si el usuario quiere reintentar)
   */
  printTicket(): void {
    window.print();
  }

  /**
   * Cierra la ventana de impresión
   */
  closeWindow(): void {
    try {
      window.close();

      setTimeout(() => {
        if (!document.hidden && document.visibilityState === 'visible') {
          try {
            window.location.href = 'about:blank';
          } catch (e) {
            this.logger.warn(
              'No se pudo cerrar la ventana. Por favor, ciérrela manualmente.'
            );
          }
        }
      }, 100);
    } catch (error) {
      this.logger.warn('Error al cerrar ventana', error);
      try {
        window.location.href = 'about:blank';
      } catch (e) {
        this.logger.error('No se pudo cerrar la ventana', e);
      }
    }
  }

  /**
   * Obtiene el nombre del transportista
   */
  getCarrierName(): string {
    return this.guide?.carrierDto?.name || 'N/A';
  }

  /**
   * Obtiene la identificación del transportista
   */
  getCarrierIdentification(): string {
    return this.guide?.carrierDto?.identification || 'N/A';
  }

  /**
   * Obtiene el nombre del destinatario
   */
  getRecipientName(): string {
    return this.guide?.clientDto?.name || 'N/A';
  }

  /**
   * Obtiene la identificación del destinatario
   */
  getRecipientIdentification(): string {
    return this.guide?.clientDto?.identification || 'N/A';
  }

  /**
   * Obtiene el tipo de transporte
   */
  getTransportType(): string {
    const transportTypes: { [key: string]: string } = {
      '01': 'Terrestre',
      '02': 'Marítimo',
      '03': 'Aéreo',
    };
    return transportTypes[this.guide?.transportType || ''] || this.guide?.transportType || 'N/A';
  }

  /**
   * Obtiene el motivo de traslado
   */
  getReasonForTransfer(): string {
    const reasons: { [key: string]: string } = {
      '01': 'Venta',
      '02': 'Compra',
      '03': 'Traslado entre establecimientos',
      '04': 'Exportación',
      '05': 'Importación',
      '06': 'Devolución',
      '07': 'Consignación',
      '08': 'Remisión',
      '09': 'Demás',
    };
    return reasons[this.guide?.reasonForTransfer || ''] || this.guide?.reasonForTransfer || 'N/A';
  }

  /**
   * Verifica si hay referencias a documentos
   */
  hasDocumentReferences(): boolean {
    return !!(this.guide?.documentReferences && this.guide.documentReferences.length > 0);
  }
}

