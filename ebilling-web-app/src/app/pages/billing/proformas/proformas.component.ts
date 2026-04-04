import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Injectable,
} from '@angular/core';
import { MatPaginator, MatPaginatorIntl } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ToastService } from '../../../_service/notification/toast.service';
import { MatDialog } from '@angular/material/dialog';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { ProformaService } from '../../../_service/proforma/proforma.service';
import {
  ProformaListResponseDto,
  ProformaStatus,
} from '../../../_model/proforma/proforma-dto';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ConfirmDialogComponent } from '../../../_shared/confirm-dialog/confirm-dialog.component';
import { environment } from '../../../../environments/environment';
import { Router } from '@angular/router';

@Injectable()
export class ProformasPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Items por página:';
  override nextPageLabel = 'Siguiente';
  override previousPageLabel = 'Anterior';
  override firstPageLabel = 'Primera página';
  override lastPageLabel = 'Última página';

  override getRangeLabel = (
    page: number,
    pageSize: number,
    length: number,
  ): string => {
    if (length === 0 || pageSize === 0) {
      return `0 de ${length} proformas`;
    }
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `Mostrando ${startIndex + 1}-${endIndex} de ${length} proformas`;
  };
}

@Component({
  selector: 'app-proformas',
  templateUrl: './proformas.component.html',
  styleUrls: ['./proformas.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: MatPaginatorIntl, useClass: ProformasPaginatorIntl }],
})
export class ProformasComponent implements OnInit, AfterViewInit, OnDestroy {
  searchForm: FormGroup;
  displayedColumns = [
    'proformaNumber',
    'issueDate',
    'validUntil',
    'clientIdentification',
    'clientName',
    'totalAmount',
    'status',
    'actions',
  ];
  dataSource = new MatTableDataSource<ProformaListResponseDto>([]);
  pageSize = 10;
  totalElements = 0;
  currentPage = 0;
  isLoading = false;

  statusOptions: { value: ProformaStatus; label: string }[] = [
    { value: 'DRAFT', label: 'Borrador' },
    { value: 'SENT', label: 'Enviada' },
    { value: 'CONVERTED', label: 'Convertida' },
    { value: 'EXPIRED', label: 'Expirada' },
  ];

  private destroy$ = new Subject<void>();

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private readonly proformaService: ProformaService,
    private readonly toast: ToastService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly fb: FormBuilder,
    private readonly logger: LoggerService,
    private readonly router: Router,
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.logger.debug('ProformasComponent initialized');
    this.search();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    if (this.paginator) {
      this.paginator.page
        .pipe(takeUntil(this.destroy$))
        .subscribe((pageEvent) => {
          this.currentPage = pageEvent.pageIndex;
          this.pageSize = pageEvent.pageSize;
          this.search();
        });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    const today = new Date();
    const nextMonth = new Date(today);
    nextMonth.setMonth(nextMonth.getMonth() + 1);

    this.searchForm = this.fb.group({
      issueDateFrom: [this.formatDateForInput(today)],
      issueDateTo: [this.formatDateForInput(nextMonth)],
      status: [''],
      identification: [''],
    });
  }

  private formatDateForInput(d: Date): string {
    return d.toISOString().split('T')[0];
  }

  search(): void {
    const idCompanyStr = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyStr) {
      this.logger.error('No se encontró ID de compañía');
      this.toast.error(
        'Error: No se encontró información de la compañía. Por favor, inicie sesión nuevamente.',
      );
      return;
    }

    const idCompany = Number(idCompanyStr);
    if (!idCompany || isNaN(idCompany)) {
      this.toast.error('ID de compañía inválido.');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    const formVal = this.searchForm.value;
    const toDateStr = (
      v: Date | string | null | undefined,
    ): string | undefined => {
      if (!v) return undefined;
      if (typeof v === 'string') return v || undefined;
      return v.toISOString().split('T')[0];
    };
    const params = {
      issueDateFrom: toDateStr(formVal.issueDateFrom) || undefined,
      issueDateTo: toDateStr(formVal.issueDateTo) || undefined,
      status: formVal.status || undefined,
      identification: formVal.identification || undefined,
      sort: 'issueDate,desc',
    };

    this.proformaService
      .search(idCompany, this.currentPage, this.pageSize, params)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.logger.error('Error searching proformas', error);
          this.isLoading = false;
          this.dataSource.data = [];
          this.totalElements = 0;
          this.cdr.markForCheck();
          this.toast.error('Error al buscar proformas. Intente nuevamente.');
          return of({
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: this.pageSize,
          });
        }),
      )
      .subscribe((response) => {
        this.isLoading = false;
        this.dataSource.data = response.content || [];
        this.totalElements = response.totalElements ?? 0;
        this.cdr.markForCheck();

        if (this.dataSource.data.length === 0 && !formVal.status) {
          this.toast.info('No se encontraron proformas');
        }
      });
  }

  clearFilters(): void {
    this.searchForm.reset({
      issueDateFrom: '',
      issueDateTo: '',
      status: '',
      identification: '',
    });
    this.currentPage = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    this.search();
  }

  createProforma(): void {
    this.router.navigate(['/invoices/proformas/create']);
  }

  editProforma(row: ProformaListResponseDto): void {
    if (row.status !== 'DRAFT') {
      this.toast.warning('Solo se pueden editar proformas en estado Borrador');
      return;
    }
    this.router.navigate(['/invoices/proformas/edit', row.idProforma]);
  }

  deleteProforma(row: ProformaListResponseDto): void {
    if (row.status !== 'DRAFT') {
      this.toast.warning(
        'Solo se pueden eliminar proformas en estado Borrador',
      );
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: '¿Eliminar proforma?',
        message: `¿Está seguro de eliminar la proforma ${row.proformaNumber}? Esta acción no se puede deshacer.`,
        confirmText: 'Eliminar',
        cancelText: 'Cancelar',
        type: 'danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;

      const idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
      if (!idCompany) {
        this.toast.error('Error: No se encontró compañía.');
        return;
      }

      this.isLoading = true;
      this.cdr.markForCheck();

      this.proformaService.delete(row.idProforma, idCompany).subscribe({
        next: () => {
          this.toast.success('Proforma eliminada exitosamente');
          this.search();
        },
        error: (err) => {
          this.logger.error('Error deleting proforma', err);
          this.isLoading = false;
          this.cdr.markForCheck();
          this.toast.error(
            'Error al eliminar la proforma. Intente nuevamente.',
          );
        },
      });
    });
  }

  convertToInvoice(row: ProformaListResponseDto): void {
    if (row.status !== 'DRAFT') {
      this.toast.warning(
        'Solo se pueden convertir a factura proformas en estado Borrador',
      );
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '500px',
      maxWidth: '90vw',
      data: {
        title: 'Convertir a factura',
        message: `¿Desea convertir la proforma ${row.proformaNumber} en una factura borrador?`,
        confirmText: 'Convertir',
        cancelText: 'Cancelar',
        type: 'primary',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;

      const idCompany = Number(localStorage.getItem(environment.ID_COMPANY));
      if (!idCompany) {
        this.toast.error('Error: No se encontró compañía.');
        return;
      }

      this.isLoading = true;
      this.cdr.markForCheck();

      this.proformaService
        .convertToInvoice(row.idProforma, idCompany)
        .subscribe({
          next: (res) => {
            const invoiceNum =
              res.invoiceNumber ??
              (res as { invoiceDto?: { invoiceNumber?: string } }).invoiceDto
                ?.invoiceNumber;
            const msg = invoiceNum
              ? `Proforma convertida. Factura: ${invoiceNum}`
              : 'Proforma convertida. Se abrirá el borrador para continuar.';
            this.toast.success(msg);
            this.search();
            if (res.idInvoice) {
              this.router.navigate(['/invoices/create'], {
                queryParams: { draftId: res.idInvoice },
              });
            }
          },
          error: (err) => {
            this.logger.error('Error converting proforma', err);
            this.isLoading = false;
            this.cdr.markForCheck();
            this.toast.error(
              'Error al convertir la proforma. Intente nuevamente.',
            );
          },
        });
    });
  }

  printProforma(row: ProformaListResponseDto): void {
    const idCompanyStr = localStorage.getItem(environment.ID_COMPANY);
    if (!idCompanyStr) {
      this.toast.error(
        'Error: No se encontró información de la compañía. Por favor, inicie sesión nuevamente.',
      );
      return;
    }

    const idCompany = Number(idCompanyStr);
    if (!idCompany || isNaN(idCompany)) {
      this.toast.error('ID de compañía inválido.');
      return;
    }

    this.proformaService
      .getPdf(row.idProforma, idCompany)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          const fileURL = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = fileURL;
          a.download =
            (row.proformaNumber || `proforma-${row.idProforma}`) + '.pdf';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(fileURL);
        },
        error: (err) => {
          this.logger.error('Error descargando PDF de proforma', err);
          this.toast.error(
            'Error al generar el PDF de la proforma. Intente nuevamente.',
          );
        },
      });
  }

  isDraft(row: ProformaListResponseDto): boolean {
    return row.status === 'DRAFT';
  }

  formatDate(date: string | undefined): string {
    if (!date) return '-';
    try {
      return new Date(date).toLocaleDateString('es-EC', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      });
    } catch {
      return '-';
    }
  }

  formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '$0.00';
    return `$${amount.toFixed(2)}`;
  }

  getStatusClass(row: ProformaListResponseDto): string {
    switch (row.status) {
      case 'DRAFT':
        return 'status-draft';
      case 'SENT':
        return 'status-sent';
      case 'CONVERTED':
        return 'status-converted';
      case 'EXPIRED':
        return 'status-expired';
      default:
        return '';
    }
  }

  getStatusText(row: ProformaListResponseDto): string {
    const opt = this.statusOptions.find((o) => o.value === row.status);
    return opt?.label ?? row.status ?? '-';
  }
}
