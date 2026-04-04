import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { PlanResponse } from '../../../../_model/subscription/subscription-admin';
import { SubscriptionAdminService } from '../../../../_service/subscription/subscription-admin.service';
import { PlanFormDialogComponent } from '../dialogs/plan-form-dialog/plan-form-dialog.component';
import { ToastService } from '../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../_service/logger/logger.service';

@Component({
  selector: 'app-plan-list',
  templateUrl: './plan-list.component.html',
  styleUrls: ['./plan-list.component.css'],
})
export class PlanListComponent implements OnInit, AfterViewInit {
  displayedColumns: string[] = [
    'code',
    'name',
    'priceAnnual',
    'priceMonthly',
    'documentLimit',
    'userLimit',
    'isActive',
    'displayOrder',
    'actions',
  ];

  dataSource = new MatTableDataSource<PlanResponse>([]);
  isLoading = false;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  // Filtros
  filterText = '';
  includeInactive = false;

  constructor(
    private readonly subscriptionService: SubscriptionAdminService,
    private readonly dialog: MatDialog,
    private readonly toast: ToastService,
    private readonly logger: LoggerService
  ) {
    this.dataSource = new MatTableDataSource<PlanResponse>([]);
  }

  ngOnInit(): void {
    this.loadPlans();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadPlans(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      console.warn('[PlanList] No hay token, omitiendo carga de datos');
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    this.subscriptionService.getAllPlansFull(this.includeInactive).subscribe({
      next: (plans) => {
        this.dataSource.data = plans.sort(
          (a, b) => a.displayOrder - b.displayOrder
        );
        this.isLoading = false;
        if (this.dataSource.paginator) {
          this.dataSource.paginator.firstPage();
        }
      },
      error: (error) => {
        console.error('Error cargando planes:', error);
        this.toast.error('Error al cargar los planes');
        this.isLoading = false;
      },
    });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(PlanFormDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: null, // null = crear nuevo
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.loadPlans();
      }
    });
  }

  openEditDialog(plan: PlanResponse): void {
    const dialogRef = this.dialog.open(PlanFormDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      maxHeight: '90vh',
      disableClose: false,
      autoFocus: false,
      data: plan, // Plan existente para editar
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result === true) {
        this.loadPlans();
      }
    });
  }

  deletePlan(plan: PlanResponse): void {
    if (
      !confirm(
        `¿Está seguro que desea eliminar el plan "${plan.name}" (${plan.code})?`
      )
    ) {
      return;
    }

    this.subscriptionService.deletePlan(plan.idPlan).subscribe({
      next: () => {
        this.toast.success(`Plan "${plan.name}" eliminado exitosamente`);
        this.loadPlans();
      },
      error: (error) => {
        console.error('Error eliminando plan:', error);
        const errorMessage =
          error.error?.message || error.message || 'Error al eliminar el plan';
        this.toast.error(errorMessage);
      },
    });
  }

  toggleActive(plan: PlanResponse): void {
    const newStatus = !plan.isActive;
    this.subscriptionService
      .updatePlan(plan.idPlan, { isActive: newStatus })
      .subscribe({
        next: () => {
          this.toast.success(
            `Plan "${plan.name}" ${
              newStatus ? 'activado' : 'desactivado'
            } exitosamente`
          );
          this.loadPlans();
        },
        error: (error) => {
          console.error('Error actualizando plan:', error);
          const errorMessage =
            error.error?.message ||
            error.message ||
            'Error al actualizar el plan';
          this.toast.error(errorMessage);
        },
      });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  onIncludeInactiveChange(): void {
    this.loadPlans();
  }

  formatLimit(limit: number | null): string {
    if (limit === null || limit === -1) {
      return 'Ilimitado';
    }
    return limit.toLocaleString();
  }

  formatPrice(price: number): string {
    return `$${price.toFixed(2)}`;
  }
}
