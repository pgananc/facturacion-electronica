import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DebitNoteService } from '../../../../_service/invoice/debit-note.service';
import { DebitNoteDto } from '../../../../_model/invoice/debit-note-dto';
import { InvoiceDetailDto } from '../../../../_model/invoice/invoice-dto';
import { environment } from '../../../../../environments/environment';
import { LoggerService } from '../../../../_service/logger/logger.service';

@Component({
  selector: 'app-debit-note-ticket-print',
  templateUrl: './debit-note-ticket-print.component.html',
  styleUrls: ['./debit-note-ticket-print.component.css'],
})
export class DebitNoteTicketPrintComponent
  implements OnInit, AfterViewInit, OnDestroy
{
  debitNote: DebitNoteDto | null = null;
  isLoading = true;
  error: string | null = null;

  // Datos de la empresa desde localStorage
  companyName: string = '';
  companyRuc: string = '';
  companyPhone: string = '';
  companyAddress: string = '';

  // ✅ Subtotales calculados desde debitNoteDetails
  subtotalsByIva: { ivaPercent: number; subtotal: number; ivaValue: number }[] =
    [];
  subtotalSinImpuestos: number = 0;
  totalDescuento: number = 0;
  totalICE: number = 0;
  totalIVA: number = 0;

  // Handler para el evento afterprint (cierre automático después de imprimir)
  private afterPrintHandler?: () => void;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private debitNoteService: DebitNoteService,
    private logger: LoggerService
  ) {}

  ngOnInit(): void {
    const debitNoteId = this.route.snapshot.paramMap.get('debitNoteId');

    if (!debitNoteId) {
      this.error = 'No se proporcionó ID de nota de débito';
      this.isLoading = false;
      return;
    }

    this.loadCompanyData();
    this.loadDebitNoteOptimized(Number(debitNoteId));
  }

  ngAfterViewInit(): void {
    // Solo auto-imprimir si no hay error y la nota de débito está cargada
    // Esperar a que el componente termine de cargar antes de intentar imprimir
    setTimeout(() => {
      // Si hay error o aún está cargando, no intentar imprimir
      if (this.error || this.isLoading) {
        return;
      }

      // ✅ Asegurar que el título esté actualizado antes de imprimir
      this.setDocumentTitle();

      // Configurar el listener para cerrar la ventana después de que se cierre el diálogo de impresión
      this.afterPrintHandler = () => {
        // Cerrar automáticamente después de un breve delay
        // Esto permite que el diálogo se cierre completamente antes de cerrar la ventana
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
   * ✅ MÉTODO OPTIMIZADO: Carga la nota de débito desde sessionStorage si existe, sino desde el backend
   */
  private loadDebitNoteOptimized(debitNoteId: number): void {
    const storageKey = `DEBIT_NOTE_TICKET_${debitNoteId}`;
    const storedData = sessionStorage.getItem(storageKey);

    if (storedData) {
      // ✅ DATOS DISPONIBLES EN sessionStorage (caso común: impresión automática)
      try {
        // El dato guardado es directamente el DebitNoteDto completo (ya combinado)
        // Similar a cómo se guarda en billing.component.ts con mergeInvoiceData
        this.debitNote = JSON.parse(storedData);

        this.logger.debug('✅ Nota de débito cargada desde sessionStorage', {
          debitNoteId,
          issueDate: this.debitNote?.issueDate,
          originalIssueDate: this.debitNote?.originalIssueDate,
          hasDebitNoteNumber: !!this.debitNote?.debitNoteNumber,
          hasClientDto: !!this.debitNote?.clientDto,
          clientDto: this.debitNote?.clientDto,
          detailsCount: this.getDebitNoteDetails().length,
          details: this.getDebitNoteDetails(),
        });

        this.completeDebitNoteData();
        this.isLoading = false;
      } catch (error) {
        this.logger.error('Error al parsear datos de sessionStorage', error);
        // Si hay error al parsear, intentar cargar desde el backend
        this.loadDebitNoteFromBackend(debitNoteId);
      }
    } else {
      // ✅ FALLBACK: Cargar desde el backend (caso de recarga de página o acceso directo)
      this.loadDebitNoteFromBackend(debitNoteId);
    }
  }

  /**
   * ✅ MÉTODO: Carga la nota de débito desde el backend (fallback)
   */
  private loadDebitNoteFromBackend(debitNoteId: number): void {
    this.debitNoteService.findById(debitNoteId).subscribe({
      next: (response) => {
        // El backend puede retornar directamente el DTO o envuelto en un response
        // Verificar si viene envuelto en una estructura con code/data
        if (response) {
          let debitNoteData: DebitNoteDto | null = null;

          // Si tiene estructura { code: 1, data: {...} }
          if ((response as any)?.code === 1 && (response as any)?.data) {
            debitNoteData = (response as any).data;
          }
          // Si tiene estructura { debitNoteDto: {...} }
          else if ((response as any)?.debitNoteDto) {
            debitNoteData = (response as any).debitNoteDto;
          }
          // Si es directamente el DTO
          else {
            debitNoteData = response as DebitNoteDto;
          }

          if (debitNoteData) {
            this.debitNote = debitNoteData;
            this.logger.debug('✅ Nota de débito cargada desde backend', {
              debitNoteId,
              issueDate: this.debitNote?.issueDate,
              originalIssueDate: this.debitNote?.originalIssueDate,
              hasDebitNoteNumber: !!this.debitNote?.debitNoteNumber,
              hasClientDto: !!this.debitNote?.clientDto,
              clientDto: this.debitNote?.clientDto,
              detailsCount: this.getDebitNoteDetails().length,
              details: this.getDebitNoteDetails(),
              debitNoteKeys: Object.keys(this.debitNote || {}),
            });
            this.completeDebitNoteData();
            this.isLoading = false;
          } else {
            this.error = 'No se pudo cargar la nota de débito';
            this.isLoading = false;
            this.cancelAutoPrint();
          }
        } else {
          this.error = 'No se pudo cargar la nota de débito';
          this.isLoading = false;
          this.cancelAutoPrint();
        }
      },
      error: (err) => {
        this.logger.error(
          '❌ Error al cargar nota de débito para impresión',
          err
        );

        // Manejar errores específicos sin cerrar sesión
        if (err.status === 401 || err.status === 403) {
          this.error =
            'No tiene permisos para acceder a esta nota de débito. Por favor, inicie sesión e intente nuevamente.';
        } else if (err.status === 404) {
          this.error = 'La nota de débito solicitada no existe.';
        } else if (err.status === 0) {
          // Error de conexión o CORS
          this.error =
            'No se pudo conectar con el servidor. Verifique su conexión e intente nuevamente.';
        } else {
          this.error =
            'Error al cargar la nota de débito. Por favor, intente nuevamente.';
        }

        this.isLoading = false;
        this.cancelAutoPrint();
      },
    });
  }

  /**
   * ✅ MÉTODO: Completa los datos de la nota de débito (dirección, nombre empresa, cálculos)
   */
  private completeDebitNoteData(): void {
    if (!this.debitNote) return;

    // Completar dirección si no se encontró en contactos
    if (!this.companyAddress && this.debitNote.establishment?.address) {
      this.companyAddress = this.debitNote.establishment.address;
    }

    // Usar nombre de la empresa del debitNote si está disponible
    if (this.debitNote.companyDto?.name) {
      this.companyName = this.debitNote.companyDto.name;
    }

    // ✅ CALCULAR SUBTOTALES POR % DE IVA
    this.calculateSubtotalsByIva();

    // ✅ Establecer título de página con identificación del cliente
    this.setDocumentTitle();
  }

  /**
   * ✅ CALCULA SUBTOTALES POR % DE IVA (15%, 5%, 0%)
   * Basado en los debitNoteDetails, agrupando por ivaPercentage de cada producto
   */
  private calculateSubtotalsByIva(): void {
    if (!this.debitNote) return;

    const details = this.getDebitNoteDetails();

    // Map para agrupar subtotales por % de IVA
    const subtotalsByIvaMap = new Map<number, number>([
      [15, 0], // IVA 15%
      [5, 0], // IVA 5%
      [0, 0], // IVA 0%
    ]);

    let subtotalSinImpuestos = 0;
    let totalDescuento = 0;
    let totalICE = 0;

    details.forEach((item: InvoiceDetailDto) => {
      // Obtener porcentaje de IVA del producto
      // Puede estar en ivaPercentage, productDto.rate, o calcular desde productDto
      const ivaPercent =
        item.ivaPercentage || (item.productDto as any)?.rate || 0;

      // Calcular subtotal de la línea (precio × cantidad)
      const lineSubtotal = (item.unitPrice || 0) * (item.quantity || 1);

      // Descuento en valor monetario (el backend retorna descuento en valor, no %)
      const lineDiscount = item.discount || 0;

      // Base imponible después del descuento
      const baseImponible = lineSubtotal - lineDiscount;

      // Agrupar subtotal por % de IVA
      const currentSubtotal = subtotalsByIvaMap.get(ivaPercent) || 0;
      subtotalsByIvaMap.set(ivaPercent, currentSubtotal + baseImponible);

      // Acumular totales generales
      subtotalSinImpuestos += lineSubtotal;
      totalDescuento += lineDiscount;
    });

    // Calcular IVA por cada tarifa y construir array
    this.subtotalsByIva = [];
    let totalIVA = 0;

    subtotalsByIvaMap.forEach((subtotal, ivaPercent) => {
      const ivaValue = subtotal * (ivaPercent / 100);

      const subtotalRounded = Math.round(subtotal * 100) / 100;
      const ivaValueRounded = Math.round(ivaValue * 100) / 100;

      totalIVA += ivaValueRounded;

      // Solo agregar si el subtotal es mayor a 0
      if (subtotalRounded > 0) {
        this.subtotalsByIva.push({
          ivaPercent: ivaPercent,
          subtotal: subtotalRounded,
          ivaValue: ivaValueRounded,
        });
      }
    });

    // Ordenar por % de IVA descendente (15%, 5%, 0%)
    this.subtotalsByIva.sort((a, b) => b.ivaPercent - a.ivaPercent);

    // Asignar totales
    this.subtotalSinImpuestos = Math.round(subtotalSinImpuestos * 100) / 100;
    this.totalDescuento = this.debitNote.discount || totalDescuento;
    this.totalICE = this.debitNote.subtotalIce || 0;
    this.totalIVA = Math.round(totalIVA * 100) / 100;
  }

  /**
   * Verifica si un valor numérico es mayor que 0 (maneja null/undefined y errores de precisión)
   */
  private isPositiveValue(value: number | null | undefined): boolean {
    if (value === null || value === undefined) {
      return false;
    }
    // Redondear a 2 decimales y comparar (evita errores de precisión)
    const rounded = Math.round(value * 100) / 100;
    return rounded > 0;
  }

  /**
   * Obtiene el subtotal para un % de IVA específico
   */
  getSubtotalByIva(ivaPercent: number): number {
    const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
    return item?.subtotal || 0;
  }

  /**
   * Verifica si hay subtotal para un % de IVA específico
   */
  hasSubtotalByIva(ivaPercent: number): boolean {
    return this.isPositiveValue(this.getSubtotalByIva(ivaPercent));
  }

  /**
   * Obtiene el IVA para un % específico
   */
  getIvaByPercent(ivaPercent: number): number {
    const item = this.subtotalsByIva.find((s) => s.ivaPercent === ivaPercent);
    return item?.ivaValue || 0;
  }

  /**
   * Verifica si hay IVA para un % específico
   */
  hasIvaByPercent(ivaPercent: number): boolean {
    return this.isPositiveValue(this.getIvaByPercent(ivaPercent));
  }

  /**
   * Verifica si hay descuento
   */
  hasDiscount(): boolean {
    return this.isPositiveValue(this.totalDescuento);
  }

  /**
   * Verifica si hay ICE
   */
  hasICE(): boolean {
    return this.isPositiveValue(this.totalICE);
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const day = String(date.getDate()).padStart(2, '0');
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const year = date.getFullYear();
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${day}/${month}/${year}  ${hours}:${minutes}`;
    } catch (error) {
      return dateStr;
    }
  }

  /**
   * Formatea una fecha solo con día/mes/año (sin hora)
   * Maneja correctamente fechas ISO (YYYY-MM-DD) para evitar problemas de zona horaria
   */
  formatDateOnly(dateStr: string | undefined): string {
    if (!dateStr) return '';
    try {
      // Si la fecha está en formato ISO (YYYY-MM-DD o YYYY-MM-DDTHH:mm:ss.sssZ)
      // Extraer solo la parte de la fecha para evitar problemas de zona horaria
      if (dateStr.includes('T')) {
        // Fecha con hora: extraer solo la parte de la fecha
        const datePart = dateStr.split('T')[0];
        const [year, month, day] = datePart.split('-');
        return `${day}/${month}/${year}`;
      } else if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
        // Fecha ISO sin hora (YYYY-MM-DD)
        const [year, month, day] = dateStr.split('-');
        return `${day}/${month}/${year}`;
      } else {
        // Intentar parsear como Date y formatear
        const date = new Date(dateStr);
        // Verificar que la fecha sea válida
        if (isNaN(date.getTime())) {
          return dateStr;
        }
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        return `${day}/${month}/${year}`;
      }
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

  calculateChange(): number {
    if (!this.debitNote?.paymentMethods) return 0;

    const cashPayment = this.debitNote.paymentMethods.find(
      (p) =>
        p.paymentMethodCode === '01' ||
        p.paymentType?.toLowerCase().includes('efectivo')
    );

    if (!cashPayment) return 0;

    const paid = cashPayment.total || cashPayment.value || 0;
    const total = this.debitNote.valorTotal || this.debitNote.totalAmount || 0;
    return Math.max(0, paid - total);
  }

  getIdentificationTypeName(
    idType: number | undefined,
    identification?: string
  ): string {
    if (!idType) {
      // Si no hay idType pero hay identificación, intentar determinarlo por la longitud
      if (identification) {
        const idLength = identification.trim().length;
        if (idLength === 13) return 'RUC';
        if (idLength === 10) return 'CEDULA';
      }
      return '';
    }

    // Si es CONSUMIDOR FINAL (4) pero tiene identificación, mostrar RUC o CEDULA según la longitud
    if (idType === 4 && identification) {
      const idLength = identification.trim().length;
      if (idLength === 13) return 'RUC';
      if (idLength === 10) return 'CEDULA';
      // Si no coincide con ningún formato conocido, mostrar CONSUMIDOR FINAL
      return 'CONSUMIDOR FINAL';
    }

    const types: { [key: number]: string } = {
      1: 'RUC',
      2: 'CEDULA',
      3: 'PASAPORTE',
      4: 'CONSUMIDOR FINAL',
    };
    return types[idType] || 'OTRO';
  }

  getDebitNoteDetails() {
    return this.debitNote?.debitNoteDetails || this.debitNote?.details || [];
  }

  getPayments() {
    return this.debitNote?.paymentMethods || [];
  }

  /**
   * ✅ Verifica si la nota de débito tiene número (está autorizada)
   */
  hasDebitNoteNumber(): boolean {
    return !!this.debitNote?.debitNoteNumber;
  }

  /**
   * Establece el título de la página con el número de identificación del cliente
   * Esto se usará como nombre de archivo cuando se guarde como PDF
   */
  private setDocumentTitle(): void {
    if (!this.debitNote?.clientDto?.identification) {
      // Si no hay identificación, usar el número de nota de débito o ID
      const debitNoteId = this.route.snapshot.paramMap.get('debitNoteId');
      const fallback = this.debitNote?.debitNoteNumber || `NotaDebito-${debitNoteId}`;
      document.title = `Ticket-${fallback}`;
      return;
    }

    // Limpiar caracteres especiales que pueden causar problemas en nombres de archivo
    const cleanIdentification = this.debitNote.clientDto.identification
      .replace(/[^a-zA-Z0-9-]/g, '_');
    
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
   * Si window.close() no funciona, solo intenta cerrar sin redirigir
   */
  closeWindow(): void {
    try {
      // Intentar cerrar la ventana directamente
      // Si la ventana fue abierta por JavaScript (window.open), esto funcionará
      window.close();

      // Si después de un breve delay la ventana sigue abierta, solo mostrar un mensaje
      // NO redirigir porque podría afectar la ventana principal
      setTimeout(() => {
        // Verificar si la ventana sigue visible
        if (!document.hidden && document.visibilityState === 'visible') {
          // Si window.close() no funcionó, intentar about:blank como último recurso
          // Esto cierra la ventana sin redirigir al login
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
      // Último intento: about:blank (no redirige al login)
      try {
        window.location.href = 'about:blank';
      } catch (e) {
        this.logger.error('No se pudo cerrar la ventana', e);
      }
    }
  }
}
