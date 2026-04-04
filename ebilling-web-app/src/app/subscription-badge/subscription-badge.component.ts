import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  Input,
} from '@angular/core';
import { Subject, interval } from 'rxjs';
import { takeUntil, startWith, switchMap } from 'rxjs/operators';
import { PermissionService } from '../_service/permission/permission.service';
import { ToastService } from '../_service/notification/toast.service';
import { CompanySubscription } from '../_model/subscription/company-subscription';
import { SubscriptionUsageResponse } from '../_model/subscription/subscription-usage';

@Component({
  selector: 'app-subscription-badge',
  templateUrl: './subscription-badge.component.html',
  styleUrls: ['./subscription-badge.component.css'],
})
export class SubscriptionBadgeComponent implements OnInit, OnDestroy {
  @Input() compactMode = false; // Modo compacto para toolbar

  subscription: CompanySubscription | null = null;
  usage: SubscriptionUsageResponse | null = null;
  percentUsed = 0;
  isNearLimit = false; // >80%
  isAtLimit = false; // 100%
  alertShown80 = false;
  alertShown100 = false;

  private destroy$ = new Subject<void>();
  private readonly REFRESH_INTERVAL = 60000; // 1 minuto
  private isLoadingUsage = false; // Guard para evitar llamadas duplicadas

  constructor(
    private readonly permissionService: PermissionService,
    private readonly toastService: ToastService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Escuchar cambios en la suscripción
    this.permissionService
      .getSubscription()
      .pipe(takeUntil(this.destroy$))
      .subscribe((subscription) => {
        this.subscription = subscription;
        // Cargar usage cuando cambie la suscripción
    this.loadSubscriptionData();
      });

    // Refrescar datos cada minuto (solo si hay suscripción)
    interval(this.REFRESH_INTERVAL)
      .pipe(
        startWith(0),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        // Solo cargar usage si hay suscripción y no hay una llamada en curso
        if (this.subscription?.idCompany && !this.isLoadingUsage) {
        this.loadSubscriptionData();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadSubscriptionData(): void {
    // Cargar suscripción
    this.subscription = this.permissionService.getSubscriptionValue();

    // Cargar uso de documentos
    const idCompany = this.subscription?.idCompany;
    if (idCompany) {
      // Guard: evitar llamadas duplicadas
      if (this.isLoadingUsage) {
        return;
      }

      this.isLoadingUsage = true;
      this.permissionService
        .getUsage(idCompany)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (usage) => {
        this.usage = usage;
        this.calculatePercentUsed();
        this.checkLimits();
        this.cdr.detectChanges();
            this.isLoadingUsage = false;
          },
          error: () => {
            this.isLoadingUsage = false;
            this.cdr.detectChanges();
          },
      });
    } else {
      this.usage = null;
      this.percentUsed = 0;
      this.cdr.detectChanges();
    }
  }

  private calculatePercentUsed(): void {
    if (!this.usage?.currentYear) {
      this.percentUsed = 0;
      return;
    }

    const { total, limit } = this.usage.currentYear;
    if (!limit || limit === 0) {
      // Sin límite (plan ilimitado)
      this.percentUsed = 0;
      return;
    }

    this.percentUsed = Math.min(100, Math.round((total / limit) * 100));
  }

  private checkLimits(): void {
    this.isNearLimit = this.percentUsed >= 80 && this.percentUsed < 100;
    this.isAtLimit = this.percentUsed >= 100;

    // Mostrar alerta cuando alcanza 80%
    if (this.isNearLimit && !this.alertShown80) {
      this.alertShown80 = true;
      this.toastService.warning(
        `⚠️ Has usado el ${this.percentUsed}% de tus documentos. Considera actualizar tu plan.`
      );
    }

    // Mostrar alerta cuando alcanza 100%
    if (this.isAtLimit && !this.alertShown100) {
      this.alertShown100 = true;
      this.toastService.error(
        `🚫 Has alcanzado el límite de documentos de tu plan. Actualiza tu suscripción para continuar.`
      );
    }

    // Resetear flags si baja del umbral
    if (this.percentUsed < 80) {
      this.alertShown80 = false;
    }
    if (this.percentUsed < 100) {
      this.alertShown100 = false;
    }
  }

  get planName(): string {
    return this.subscription?.plan?.name || 'Sin Plan';
  }

  get planCode(): string {
    return this.subscription?.plan?.code || 'FREE';
  }

  get planColor(): string {
    const code = this.planCode.toUpperCase();
    switch (code) {
      case 'FREE':
        return '#9e9e9e'; // Gris
      case 'BASIC':
        return '#2196f3'; // Azul
      case 'PRO':
      case 'PROFESSIONAL':
        return '#ff9800'; // Naranja/Dorado
      case 'ENT':
      case 'ENTERPRISE':
        return '#9c27b0'; // Púrpura
      default:
        return '#9e9e9e';
    }
  }

  get documentsUsed(): number {
    return this.usage?.currentYear?.total || 0;
  }

  get documentsLimit(): number | null {
    return this.usage?.currentYear?.limit || null;
  }

  get documentsRemaining(): number {
    if (!this.documentsLimit) {
      return -1; // Ilimitado
    }
    return Math.max(0, this.documentsLimit - this.documentsUsed);
  }

  get progressBarColor(): string {
    if (this.isAtLimit) {
      return '#f44336'; // Rojo
    }
    if (this.isNearLimit) {
      return '#ff9800'; // Naranja
    }
    return '#4caf50'; // Verde
  }

  get hasUnlimitedPlan(): boolean {
    return !this.documentsLimit || this.documentsLimit === 0;
  }
}
