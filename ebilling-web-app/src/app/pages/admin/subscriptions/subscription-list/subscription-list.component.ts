import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { delay, take } from 'rxjs/operators';
import { SubscriptionAdminService } from '../../../../_service/subscription/subscription-admin.service';
import { SubscriptionAdmin } from '../../../../_model/subscription/subscription-admin';
import { UpgradePlanDialogComponent } from '../dialogs/upgrade-plan-dialog/upgrade-plan-dialog.component';
import { ExtendSubscriptionDialogComponent } from '../dialogs/extend-subscription-dialog/extend-subscription-dialog.component';
import { AssignSubscriptionDialogComponent } from '../dialogs/assign-subscription-dialog/assign-subscription-dialog.component';
import { SubscriptionDetailDialogComponent } from '../dialogs/subscription-detail-dialog/subscription-detail-dialog.component';
import { CancelSubscriptionDialogComponent } from '../dialogs/cancel-subscription-dialog/cancel-subscription-dialog.component';
import { ToastService } from '../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
@Component({
  selector: 'app-subscription-list',
  templateUrl: './subscription-list.component.html',
  styleUrls: ['./subscription-list.component.css'],
})
export class SubscriptionListComponent implements OnInit, AfterViewInit {
  displayedColumns: string[] = [
    'businessName',
    'ruc',
    'plan',
    'status',
    'usage',
    'remaining',
    'expiresAt',
    'actions',
  ];

  dataSource = new MatTableDataSource<SubscriptionAdmin>([]);
  isLoading = false;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  // Filtros
  filterPlan = '';
  filterStatus = '';

  constructor(
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    private readonly toast: ToastService,
    private readonly logger: LoggerService
  ) {
    this.dataSource = new MatTableDataSource<SubscriptionAdmin>([]);
  }

  ngOnInit(): void {
    this.loadSubscriptions();
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadSubscriptions(): void {
    // 🔧 Verificar si hay token antes de cargar datos
    const token = localStorage.getItem('token');
    if (!token) {
      console.warn('[SubscriptionList] No hay token, omitiendo carga de datos');
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    this.subscriptionService.getAllSubscriptions().subscribe({
      next: (subscriptions) => {
        this.dataSource.data = subscriptions;
        this.isLoading = false;

        // ✅ Forzar actualización de la tabla
        if (this.dataSource.paginator) {
          this.dataSource.paginator.firstPage();
        }
      },
      error: (error) => {
        console.error('Error cargando suscripciones:', error);
        this.toast.error('Error al cargar las suscripciones');
        this.isLoading = false;
      },
    });
  }

  /**
   * Abre dialog para asignar/crear suscripción
   */
  openAssignDialog(): void {
    const dialogRef = this.dialog.open(AssignSubscriptionDialogComponent, {
      width: '650px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // Se asignó correctamente, recargar tabla
        this.loadSubscriptions();
      }
    });
  }

  /**
   * Cambiar plan de suscripción
   */
  changePlan(subscription: SubscriptionAdmin): void {
    const dialogRef = this.dialog.open(UpgradePlanDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: subscription,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.toast.success('Plan actualizado exitosamente');
        this.loadSubscriptions();
      }
    });
  }

  /**
   * Extender vigencia de suscripción
   */
  extendSubscription(subscription: SubscriptionAdmin): void {
    const dialogRef = this.dialog.open(ExtendSubscriptionDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: subscription,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.toast.success('Vigencia extendida exitosamente');
        this.loadSubscriptions();
      }
    });
  }

  /**
   * Cancelar suscripción (con diálogo para capturar razón y opciones)
   */
  cancelSubscription(subscription: SubscriptionAdmin): void {
    const dialogRef = this.dialog.open(CancelSubscriptionDialogComponent, {
      width: '600px',
      data: subscription,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // El diálogo maneja la cancelación internamente
        // Solo recargamos la lista si se canceló exitosamente
        this.loadSubscriptions();
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  viewDetails(subscription: SubscriptionAdmin): void {
    const dialogRef = this.dialog.open(SubscriptionDetailDialogComponent, {
      width: '700px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: subscription,
    });

    // No necesitamos hacer nada después de cerrar, solo mostrar información
    dialogRef.afterClosed().subscribe();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'status-active';
      case 'TRIAL':
        return 'status-trial';
      case 'EXPIRED':
        return 'status-expired';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }

  getUsageClass(percentUsed: number): string {
    if (percentUsed >= 90) return 'usage-critical';
    if (percentUsed >= 75) return 'usage-warning';
    return 'usage-normal';
  }
}
