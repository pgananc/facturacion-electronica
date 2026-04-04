import {
  Component,
  OnInit,
  AfterViewInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  OnDestroy,
} from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ToastService } from '../../../_service/notification/toast.service';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { GuideService } from '../../../_service/guide/guide.service';
import {
  GuideListResponseDto,
  GuideSearchResponseDto,
} from '../../../_model/guide/guide-dto';
import { LoggerService } from '../../../_service/logger/logger.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { environment } from '../../../../environments/environment';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-guide-list',
  templateUrl: './guide-list.component.html',
  styleUrls: ['./guide-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuideListComponent implements OnInit, AfterViewInit, OnDestroy {
  private destroy$ = new Subject<void>();

  dataSource: MatTableDataSource<GuideListResponseDto> =
    new MatTableDataSource<GuideListResponseDto>([]);

  displayedColumns: string[] = [
    'guideNumber',
    'issueDate',
    'buyerName',
    'buyerIdentification',
    'carrierName',
    'guideStatus',
    'actions',
  ];

  @ViewChild(MatPaginator, { static: false }) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: false }) sort!: MatSort;

  isLoading = false;
  pageSize = 20;
  quantity = 0;

  searchForm: FormGroup;

  constructor(
    private readonly guideService: GuideService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService,
    private readonly cdRef: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    private readonly fb: FormBuilder
  ) {
    this.searchForm = this.fb.group({
      guideNumber: [''],
      idClient: [null],
      idCarrier: [null],
    });
  }

  ngOnInit(): void {
    this.logger.debug('GuideListComponent initialized');
    this.loadGuides();
  }

  ngAfterViewInit(): void {
    if (this.paginator && this.sort) {
      this.dataSource.paginator = this.paginator;
      this.dataSource.sort = this.sort;

      // Configurar sorting personalizado
      this.dataSource.sortingDataAccessor = (item, property) => {
        switch (property) {
          case 'guideNumber':
            return item.guideNumber || '';
          case 'issueDate':
            return new Date(item.issueDate || '').getTime();
          case 'buyerName':
            return item.buyerName || '';
          case 'buyerIdentification':
            return (
              (item as any).carrier?.identification ||
              item.carrierIdentification ||
              ''
            );
          case 'carrierName':
            return (item as any).carrier?.name || item.carrierName || '';
          case 'guideStatus':
            return (
              (item as any).authorizationSri?.authorizationStatusSri ||
              item.authorizationStatusSri ||
              item.guideStatus ||
              ''
            );
          default:
            return (item as any)[property];
        }
      };
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Carga las guías usando el mismo servicio que search()
   */
  loadGuides(): void {
    this.search();
  }

  /**
   * Busca guías con los filtros del formulario usando el nuevo endpoint
   */
  search(): void {
    this.isLoading = true;
    this.cdRef.markForCheck();

    // Obtener idCompany de la sesión
    const idCompanyStr = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyStr) {
      this.toast.error('No se encontró la compañía en sesión');
      this.isLoading = false;
      this.cdRef.markForCheck();
      return;
    }

    const idCompany = Number(idCompanyStr);
    if (!idCompany || idCompany <= 0) {
      this.toast.error('ID de compañía inválido');
      this.isLoading = false;
      this.cdRef.markForCheck();
      return;
    }

    const guideNumber =
      this.searchForm.get('guideNumber')?.value?.trim() || undefined;

    const searchDto = {
      idCompany,
      guideNumber,
    };

    this.guideService
      .searchByCompany(searchDto)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error: HttpErrorResponse) => {
          this.logger.error('Error al buscar guías', error);
          this.isLoading = false;
          this.cdRef.markForCheck();
          this.toast.error('Error al buscar guías. Intente nuevamente.');
          return of([]);
        })
      )
      .subscribe((response) => {
        this.isLoading = false;

        this.logger.debug('Respuesta del servidor searchByCompany:', response);
        this.logger.debug('Tipo de respuesta:', typeof response);
        this.logger.debug('Es array?', Array.isArray(response));

        // Validar que la respuesta sea un array
        let guides: GuideSearchResponseDto[] = [];

        if (Array.isArray(response)) {
          guides = response;
          this.logger.debug(
            'Respuesta es un array directo, cantidad:',
            guides.length
          );
        } else if (response && Array.isArray((response as any).content)) {
          // Si viene en formato de paginación
          guides = (response as any).content;
          this.logger.debug(
            'Respuesta viene en formato paginado, cantidad:',
            guides.length
          );
        } else if (response && Array.isArray((response as any).data)) {
          // Si viene en formato con wrapper 'data'
          guides = (response as any).data;
          this.logger.debug(
            'Respuesta viene en wrapper data, cantidad:',
            guides.length
          );
        } else if (response && typeof response === 'object') {
          // Si es un objeto único, convertirlo a array
          guides = [response as any];
          this.logger.debug('Respuesta es un objeto único, convertido a array');
        } else {
          this.logger.warn('Respuesta inesperada del servidor:', response);
          guides = [];
        }

        // Mapear GuideSearchResponseDto a GuideListResponseDto para compatibilidad
        const mappedGuides = guides.map((g) =>
          this.mapSearchResponseToListItem(g)
        );
        this.dataSource.data = mappedGuides;
        this.quantity = guides.length;
        if (this.paginator) {
          this.paginator.length = guides.length;
        }
        this.cdRef.markForCheck();
      });
  }

  /**
   * Mapea GuideSearchResponseDto a GuideListResponseDto para compatibilidad con la tabla
   */
  private mapSearchResponseToListItem(
    searchResponse: GuideSearchResponseDto
  ): GuideListResponseDto {
    return {
      idGuide: searchResponse.idGuide,
      idCompany: 0, // No disponible en el response
      idClient: 0, // No disponible en el response
      idCarrier: 0, // No disponible en el response
      guideNumber: searchResponse.guideNumber,
      issueDate: searchResponse.createDate,
      startDate: searchResponse.createDate,
      endDate: searchResponse.createDate,
      originAddress: '',
      destinationAddress: '',
      transportPlate: '',
      transportType: '',
      reasonForTransfer: '',
      buyerName: searchResponse.destinatarioNombre,
      buyerIdentification: '', // No disponible en el response
      carrierName: searchResponse.carrierName,
      carrierIdentification: searchResponse.carrierIdentification,
      guideStatus: searchResponse.authorizationStatus || 'BORRADOR',
      authorizationStatusSri: searchResponse.authorizationStatus,
      emailSent: false,
      creationDate: searchResponse.createDate,
    };
  }

  /**
   * Limpia los filtros y recarga
   */
  clear(): void {
    this.searchForm.reset({
      guideNumber: '',
      idClient: null,
      idCarrier: null,
    });
    this.search();
  }

  /**
   * Maneja la paginación
   */
  onPageChange(event: any): void {
    this.pageSize = event.pageSize;
    this.loadGuides();
  }

  /**
   * Navega a la página de creación de guía
   */
  createGuide(): void {
    this.router.navigate(['/delivery-guides/new']);
  }

  /**
   * Navega a la página de edición de guía
   */
  editGuide(guide: GuideListResponseDto): void {
    if (guide.guideStatus === 'BORRADOR' && guide.idGuide) {
      this.router.navigate(['/delivery-guides/edit', guide.idGuide]);
    } else {
      this.toast.warning('Solo se pueden editar guías en estado BORRADOR');
    }
  }

  /**
   * Elimina una guía (solo borradores)
   */
  deleteGuide(guide: GuideListResponseDto): void {
    // Usar isDraft() para validar, igual que en las acciones
    if (!this.isDraft(guide)) {
      this.toast.error('Solo se pueden eliminar guías en estado BORRADOR');
      return;
    }

    if (!guide.idGuide) {
      this.toast.error('ID de guía inválido');
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Confirmar eliminación',
        message: `¿Está seguro de que desea eliminar la guía ${guide.guideNumber || guide.idGuide}? Esta acción no se puede deshacer.`,
        confirmText: 'Eliminar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.isLoading = true;
        this.cdRef.markForCheck();
        
        this.guideService.deleteGuide(guide.idGuide!).subscribe({
          next: () => {
            this.toast.success('Guía eliminada exitosamente');
            this.loadGuides();
          },
          error: (error: HttpErrorResponse) => {
            this.isLoading = false;
            this.cdRef.markForCheck();
            this.logger.error('Error al eliminar guía', error);
            
            // Mostrar mensaje de error más específico
            const errorMessage = error.error?.message || 
                                error.error?.error || 
                                'Error al eliminar guía. Intente nuevamente.';
            
            this.toast.error(errorMessage);
          },
        });
      }
    });
  }

  /**
   * Descarga el PDF de una guía
   */
  downloadPdf(guide: GuideListResponseDto): void {
    if (!guide.idGuide) {
      this.toast.error('ID de guía inválido');
      return;
    }

    this.guideService.downloadPdf(guide.idGuide).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `guia-${guide.guideNumber}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.toast.success('PDF descargado exitosamente');
      },
      error: (error: HttpErrorResponse) => {
        this.logger.error('Error al descargar PDF', error);
        this.toast.error('Error al descargar PDF. Intente nuevamente.');
      },
    });
  }

  /**
   * Descarga el XML de una guía
   */
  downloadXml(guide: GuideListResponseDto): void {
    if (!guide.idGuide) {
      this.toast.error('ID de guía inválido');
      return;
    }

    this.guideService.downloadXml(guide.idGuide).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `guia-${guide.guideNumber}.xml`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.toast.success('XML descargado exitosamente');
      },
      error: (error: HttpErrorResponse) => {
        this.logger.error('Error al descargar XML', error);
        this.toast.error('Error al descargar XML. Intente nuevamente.');
      },
    });
  }

  /**
   * Obtiene el texto del estado de la guía
   */
  /**
   * Verifica si la guía es un borrador
   * Una guía marcada como autorizada pero sin número también se considera borrador
   */
  isDraft(guide: GuideListResponseDto): boolean {
    // Si es DEVUELTA, no es borrador (y no se mostrarán acciones)
    const guideStatus = guide.authorizationSri?.receptionStatusSri || 
                        (guide as any).receptionStatusSri ||
                        guide.authorizationStatusSri;
    if (guideStatus && typeof guideStatus === 'string' && guideStatus.toUpperCase() === 'DEVUELTA') {
      return false;
    }

    const status = guide.guideStatus || guide.authorizationSri?.authorizationStatusSri || guide.authorizationStatusSri;
    const hasNumber = !!(guide.guideNumber || guide.authorizationSri?.accessKey);
    
    // Si está marcado como autorizado pero no tiene número, es un borrador
    const isAuthorized = status === 'AUTORIZADO' || status === 'AUTHORIZED' || 
                         (typeof status === 'string' && status.toUpperCase() === 'AUTORIZADO');
    if (isAuthorized && !hasNumber) {
      return true; // Es un borrador aunque esté marcado como autorizado
    }
    
    // Si no hay estado, es borrador
    if (!status) return true;
    if (typeof status !== 'string') return true;
    const upperStatus = status.toUpperCase();
    return upperStatus === 'BORRADOR' || upperStatus === 'DRAFT';
  }

  /**
   * Verifica si la guía está autorizada
   * Una guía solo está realmente autorizada si tiene número asignado
   * Si es DEVUELTA, no está autorizada (no se mostrarán acciones)
   */
  isAuthorized(guide: GuideListResponseDto): boolean {
    // Si es DEVUELTA, no está autorizada (no se mostrarán acciones)
    const guideStatus = guide.authorizationSri?.receptionStatusSri || 
                        (guide as any).receptionStatusSri ||
                        guide.authorizationStatusSri;
    if (guideStatus && typeof guideStatus === 'string' && guideStatus.toUpperCase() === 'DEVUELTA') {
      return false;
    }

    const status = guide.guideStatus || guide.authorizationSri?.authorizationStatusSri || guide.authorizationStatusSri;
    const hasNumber = !!(guide.guideNumber || guide.authorizationSri?.accessKey);
    
    // Si no tiene número, no puede estar autorizada
    if (!hasNumber) return false;
    
    // Verificar si está autorizado
    if (!status) return false;
    if (typeof status !== 'string') return false;
    const upperStatus = status.toUpperCase();
    return upperStatus === 'AUTORIZADO' || upperStatus === 'AUTHORIZED';
  }

  getStatusText(status: string | undefined): string {
    if (!status) return 'N/A';
    const statusUpper = status.toUpperCase();
    const statusMap: { [key: string]: string } = {
      BORRADOR: 'Borrador',
      AUTORIZADO: 'Autorizado',
      AUTORIZADA: 'Autorizada',
      NO_AUTORIZADO: 'No Autorizado',
      NO_AUTORIZADA: 'No Autorizada',
      RECHAZADO: 'Rechazado',
      RECHAZADA: 'Rechazada',
      'EN PROCESO': 'En Proceso',
      EN_PROCESO: 'En Proceso',
      'PENDIENTE DE ENVIO': 'Pendiente de Envío',
      PENDIENTE_ENVIO: 'Pendiente de Envío',
      CADUCADA: 'Caducada',
      CADUCADO: 'Caducado',
      DEVUELTA: 'Devuelta',
      DEVUELTO: 'Devuelto',
    };
    return statusMap[statusUpper] || status;
  }

  /**
   * Obtiene la clase CSS del estado
   */
  getStatusClass(status: string | undefined): string {
    if (!status) return 'status-draft';
    const statusUpper = status.toUpperCase();
    const classMap: { [key: string]: string } = {
      BORRADOR: 'status-draft',
      AUTORIZADO: 'status-authorized',
      AUTORIZADA: 'status-authorized',
      NO_AUTORIZADO: 'status-rejected',
      NO_AUTORIZADA: 'status-rejected',
      RECHAZADO: 'status-rejected',
      RECHAZADA: 'status-rejected',
      'EN PROCESO': 'status-warning',
      EN_PROCESO: 'status-warning',
      'PENDIENTE DE ENVIO': 'status-info',
      PENDIENTE_ENVIO: 'status-info',
      CADUCADA: 'status-inactive',
      CADUCADO: 'status-inactive',
      DEVUELTA: 'status-warning',
      DEVUELTO: 'status-warning',
    };
    return classMap[statusUpper] || 'status-draft';
  }

  /**
   * Formatea una fecha para mostrar
   */
  formatDate(date: string | null | undefined): string {
    if (!date) return '-';
    try {
      const d = new Date(date);
      return d.toLocaleDateString('es-EC', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    } catch {
      return date;
    }
  }
}
