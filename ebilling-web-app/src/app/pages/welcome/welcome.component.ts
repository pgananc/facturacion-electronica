import { MenuService } from '../../_service/auth/menu.service';
import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  ElementRef,
  ChangeDetectorRef,
} from '@angular/core';
import { decodeToken } from 'src/app/_functions/functions';
import { CompanyService } from '../../_service/customer/company.service';
import { CompanyFilter } from '../../_model/customer/company-filter';
import { MatDialog } from '@angular/material/dialog';
import { CompanyDialogComponent } from './company-dialog/company-dialog.component';
import { environment } from 'src/environments/environment';
import {
  Company,
  CompanyEstablishmentClaim,
} from '../../_model/customer/company-establishment';
import { InvoicePointService } from '../../_service/auth/invoice-point.service';
import { InvoicePointSessionService } from '../../_service/auth/invoice-point-session.service';
import { InvoiceIssuancePoint } from '../../_model/customer/invoice-issuance-point';
import { ToastService } from '../../_service/notification/toast.service';
import { SelectInvoicePointDialogComponent } from './select-invoice-point-dialog/select-invoice-point-dialog.component';
import { LoggerService } from '../../_service/logger/logger.service';
import { Router } from '@angular/router';
import { UserService } from '../../_service/user/user.service';
import { Subject } from 'rxjs';
import { takeUntil, finalize, filter, take } from 'rxjs/operators';
import { CompanySubscription } from '../../_model/subscription/company-subscription';
import { DashboardService } from '../../_service/dashboard/dashboard.service';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { extractUserFriendlyErrorMessage } from '../../_utils/error-message.util';
import { PermissionService } from '../../_service/permission/permission.service';
import { UserRoleService } from '../../_service/auth/user-role.service';

// Registrar componentes de Chart.js
Chart.register(...registerables);

@Component({
  selector: 'app-welcome',
  templateUrl: './welcome.component.html',
  styleUrls: ['./welcome.component.css'],
})
export class WelcomeComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('salesChart') salesChartRef!: ElementRef<HTMLCanvasElement>;
  company: Company;
  isWait = false;
  token: any;

  // Propiedades para el nuevo diseño
  userName: string = '';
  companyName: string = '';
  isLoading: boolean = true;

  // KPIs
  metrics: {
    today: number;
    week: number;
    month: number;
    pending: number;
    pendingCount: number;
  } = { today: 0, week: 0, month: 0, pending: 0, pendingCount: 0 };
  isLoadingMetrics = false;
  private metricsLoaded = false; // Guard para evitar llamadas duplicadas

  // Datos para Fase 2
  salesTrendData: any[] = [];
  recentInvoices: any[] = [];
  private salesChart: Chart | null = null;

  // Subject para limpiar suscripciones y evitar memory leaks
  private readonly destroy$ = new Subject<void>();

  // Guardar empresas disponibles para poder reabrir el diálogo si no hay puntos disponibles
  private availableCompanies: Company[] = [];

  /** Si la compañía actual no tiene suscripción activa, no se muestran dashboards ni acciones */
  hasActiveSubscription = false;

  constructor(
    private readonly companyService: CompanyService,
    private readonly menuService: MenuService,
    private readonly invoicePointService: InvoicePointService,
    private readonly invoicePointSessionService: InvoicePointSessionService,
    private readonly toast: ToastService,
    private readonly dialog: MatDialog,
    private readonly logger: LoggerService,
    private readonly router: Router,
    private readonly userService: UserService,
    private readonly dashboardService: DashboardService,
    private readonly cdr: ChangeDetectorRef,
    private readonly permissionService: PermissionService,
    private readonly userRoleService: UserRoleService,
  ) {}

  ngOnInit(): void {
    this.logger.debug('Welcome component initialized');

    this.token = decodeToken();

    this.logger.debug('🔑 [Welcome] Token decodificado:', {
      token: this.token,
      idUser: this.token?.idUser,
      userName: this.token?.userName,
      idCompany: this.token?.idCompany,
      roles: this.token?.roles,
      companyEstablishments: this.token?.companyEstablishments,
      companyEstablishmentsCount:
        this.token?.companyEstablishments?.length || 0,
    });

    // Protección: Si no hay token válido, limpiar sesión y redirigir a login
    if (!this.token) {
      this.logger.warn(
        'No hay token válido en WelcomeComponent.ngOnInit(), limpiando sesión y redirigiendo a login',
      );
      localStorage.clear();
      this.router.navigate(['/login']);
      return;
    }

    // Cargar información del usuario y compañía
    this.loadUserInfo();

    // Cargar menús inmediatamente (solo dependen del usuario, no de compañía/establecimiento)
    this.loadMenusForUser();

    // Suscribirse a cambios de empresa (p. ej. desde el header) para actualizar banner y tarjeta
    this.companyService.companyNameChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((name) => {
        this.companyName = name || '';
        this.cdr.detectChanges();
      });

    this.companyService.companyChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((companyFilter: CompanyFilter) => {
        if (companyFilter) {
          this.companyName = companyFilter.name || '';
          this.company = {
            ...(this.company || ({} as Company)),
            idCompany: companyFilter.idCompany,
            name: companyFilter.name || '',
            identification: companyFilter.identification || '',
            nameCommercial: companyFilter.nameCommercial,
            idType: companyFilter.idType ?? 0,
            forcedToAccounting: companyFilter.forcedToAccounting ?? '',
            specialTaxpayer: 0,
            regime: companyFilter.regime,
            status: companyFilter.status ?? true,
            establishments: (this.company?.establishments as any) ?? [],
          } as Company;
          this.cdr.detectChanges();
        }
      });

    // Sin suscripción activa no se muestran dashboards ni acciones (no hay fallback a plan FREE)
    this.permissionService
      .getSubscription()
      .pipe(takeUntil(this.destroy$))
      .subscribe((sub) => {
        const active =
          sub != null &&
          sub.status === 'ACTIVE' &&
          !!sub.plan?.code;
        this.hasActiveSubscription = active;
        this.cdr.detectChanges();
      });

    let idCompany = localStorage.getItem(environment.ID_COMPANY);
    let idEstablishment = localStorage.getItem(environment.ID_ESTABLISHMENT);

    this.logger.debug('[Welcome] Estado inicial:', {
      hasIdCompany: !!idCompany,
      idCompanyValue: idCompany,
      hasIdEstablishment: !!idEstablishment,
      idEstablishmentValue: idEstablishment,
      hasToken: !!this.token,
      hasUserName: !!this.token?.userName,
      companyEstablishmentsCount:
        this.token?.companyEstablishments?.length || 0,
    });

    this.isWait = false;
    this.isLoading = true;

    // Si no hay idCompany O no hay idEstablishment, cargar compañías para seleccionar
    if (idCompany === null || idEstablishment === null) {
      this.logger.debug(
        '[Welcome] No hay idCompany o idEstablishment en localStorage, cargando compañías...',
        {
          hasIdCompany: !!idCompany,
          hasIdEstablishment: !!idEstablishment,
        },
      );
      this.loadDataCompanies();
    } else {
      this.logger.debug(
        '[Welcome] idCompany e idEstablishment encontrados en localStorage, cargando menú...',
      );
      this.isWait = true;

      // 🔄 Cargar roles de la compañía actual al iniciar ANTES de cargar el menú
      const companyId = Number(idCompany);
      if (companyId && companyId > 0 && !isNaN(companyId)) {
        this.logger.debug(
          '[Welcome] Cargando roles del usuario para la compañía actual:',
          {
            companyId: companyId,
          },
        );
        // Esperar a que los roles se carguen antes de cargar el menú
        this.userRoleService.getRolesByCompany(companyId).subscribe({
          next: (roles) => {
            this.logger.debug(
              `[Welcome] Roles cargados para compañía ${companyId}:`,
              roles,
            );
            // Una vez que los roles están cargados, cargar el menú
            if (this.token?.userName) {
              this.loadMenuAndSetCompany();
            } else {
              // Si no hay userName, detener loading
              this.isLoading = false;
              this.isWait = false;
            }
          },
          error: (error) => {
            this.logger.error(
              '[Welcome] Error al cargar roles del usuario:',
              error,
            );
            // Aún así, intentar cargar el menú si hay userName
            if (this.token?.userName) {
              this.loadMenuAndSetCompany();
            } else {
              // Si no hay userName, detener loading
              this.isLoading = false;
              this.isWait = false;
            }
          },
        });
      } else {
        // Si no hay companyId válido, cargar el menú directamente
        if (this.token?.userName) {
          this.loadMenuAndSetCompany();
        } else {
          // Si no hay userName, detener loading
          this.isLoading = false;
          this.isWait = false;
        }
      }
    }
  }

  // Cargar información del usuario y compañía
  private loadUserInfo(): void {
    if (this.token?.userName) {
      // Primero establecer el userName como fallback
      this.userName = this.token.userName;

      // Intentar cargar el nombre completo del usuario desde el backend
      this.userService
        .findUserByUserName(this.token.userName)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            if (response?.userDto?.name) {
              // Usar el nombre completo si está disponible
              this.userName = response.userDto.name;
              this.logger.debug(
                '✅ Nombre del usuario cargado:',
                this.userName,
              );
            } else {
              // Si no hay nombre, mantener el userName como fallback
              this.logger.warn(
                '⚠️ No se encontró nombre completo del usuario, usando userName',
              );
            }
          },
          error: (err) => {
            // Si hay error al cargar el nombre, mantener el userName como fallback
            this.logger.warn(
              '⚠️ Error al cargar nombre del usuario, usando userName:',
              err,
            );
            // No mostrar error al usuario, solo usar userName como fallback
          },
        });
    }

    const savedCompanyName = localStorage.getItem(environment.NAME_COMPANY);
    if (savedCompanyName) {
      this.companyName = savedCompanyName;
    }
  }

  loadDataCompanies() {
    if (
      !this.token?.companyEstablishments ||
      this.token.companyEstablishments.length === 0
    ) {
      this.logger.error('No hay companyEstablishments en token');
      this.toast.error(
        'No tiene empresas asignadas. Contacte al administrador.',
      );
      this.isLoading = false;
      this.isWait = false;
      return;
    }

    const companyIds = this.token.companyEstablishments.map(
      (c: CompanyEstablishmentClaim) => c.idCompany,
    );

    this.companyService
      .findByIds(companyIds)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (companyResponse) => {
          if (this.companyService.getCompanyDialogOpenedWithWarning()) {
            this.logger.debug(
              '[Welcome] Diálogo ya abierto con advertencia, no abrir otro desde loadDataCompanies',
            );
            this.isLoading = false;
            this.isWait = false;
            return;
          }
          this.isWait = true;

          // 🔍 LOG: Imprimir compañías recibidas del backend
          this.logger.debug('🏢 [Welcome] Compañías recibidas del backend:', {
            totalCompanies: companyResponse.companyDetailsDtos?.length,
            companies: companyResponse.companyDetailsDtos?.map((c) => ({
              id: c.idCompany,
              name: c.name,
              identification: c.identification,
              establishmentsCount: c.establishments?.length ?? 0,
              establishments: c.establishments?.map((e) => ({
                id: e.idEstablishment,
                name: e.name,
                code: e.code,
                address: e.address,
                type: e.type,
                status: e.status,
              })),
            })),
            fullResponse: companyResponse,
          });

          if (companyResponse.companyDetailsDtos.length == 1) {
            // ✅ Aunque sea 1 sola compañía, abrir el diálogo para seleccionar sucursal
            this.isLoading = false;
            this.isWait = false;
            this.openDialog(companyResponse.companyDetailsDtos);
          } else {
            // Múltiples compañías - abrir diálogo
            this.isLoading = false;
            this.isWait = false;
            this.openDialog(companyResponse.companyDetailsDtos);
          }
        },
        error: (err) => {
          // Log completo del error para debugging (solo en consola)
          this.logger.error('Error al cargar compañías:', {
            status: err?.status,
            url: err?.url,
            message: err?.error?.message,
            error: err,
          });

          // Mostrar mensaje amigable al usuario (sin detalles técnicos)
          const userMessage = extractUserFriendlyErrorMessage(
            err,
            'cargar información de compañías',
          );

          this.toast.httpError(
            err,
            'cargar información de compañías',
            userMessage,
          );
          this.isLoading = false;
          this.isWait = false;
        },
      });
  }

  openDialog(companies: Company[], warningMessage?: string) {
    // Guardar empresas disponibles para poder reabrir el diálogo si no hay puntos disponibles
    this.availableCompanies = companies;

    // Asegurar que el loading esté detenido antes de abrir el diálogo
    this.isLoading = false;
    this.isWait = false;

    const tokenAtDialog = decodeToken();

    this.logger.debug('[Welcome] Abriendo diálogo de selección de compañía:', {
      companiesCount: companies.length,
      hasWarningMessage: !!warningMessage,
      warningMessage,
      companies: companies.map((c) => ({
        id: c.idCompany,
        name: c.name,
        establishmentsCount: c.establishments?.length || 0,
      })),
      hasToken: !!tokenAtDialog,
      tokenIdUser: tokenAtDialog?.idUser,
      tokenUserName: tokenAtDialog?.userName,
    });

    const dialogRef = this.dialog.open(CompanyDialogComponent, {
      width: '95vw',
      maxWidth: '650px',
      data: warningMessage ? { companies, warningMessage } : companies,
      disableClose: true,
      hasBackdrop: true,
      backdropClass: 'company-dialog-backdrop',
      panelClass: 'company-dialog-panel',
    });

    dialogRef.afterClosed().subscribe((result) => {
      this.companyService.setCompanyDialogOpenedWithWarning(false);
      this.logger.debug('[Welcome] Diálogo de compañía cerrado:', {
        hasResult: !!result,
        result: result
          ? {
              companyId: result.company?.idCompany,
              companyName: result.company?.name,
              idEstablishment: result.idEstablishment,
            }
          : null,
      });

      if (!result) {
        this.logger.warn(
          '[Welcome] Diálogo cerrado sin seleccionar compañía. El usuario debe seleccionar una compañía para continuar.',
        );
        // Si el diálogo se cierra sin seleccionar, volver a abrirlo
        // Esto evita que el usuario quede sin compañía seleccionada
        setTimeout(() => {
          this.openDialog(companies);
        }, 100);
        return;
      }

      const { company, idEstablishment } = result;
      const currentUser = decodeToken().userName;

      // Guardar datos de la compañía inmediatamente
      localStorage.setItem(
        environment.ID_COMPANY,
        company.idCompany.toString(),
      );
      localStorage.setItem(environment.IDENTIFICATION, company.identification);
      localStorage.setItem(environment.NAME_COMPANY, company.name);

      // Guardar establecimiento si se proporcionó
      if (idEstablishment) {
        localStorage.setItem(
          environment.ID_ESTABLISHMENT,
          idEstablishment.toString(),
        );
      }

      // 🔄 Limpiar caché de roles de la compañía anterior (si existe)
      this.userRoleService.clearCache();

      // ✅ CARGAR MENÚS INMEDIATAMENTE después de seleccionar la compañía
      // Los permisos son a nivel de compañía, no de sucursal

      // 🔄 Cargar roles del usuario en la compañía seleccionada
      this.logger.debug(
        '[Welcome] Cargando roles del usuario para la compañía seleccionada:',
        {
          companyId: company.idCompany,
          companyName: company.name,
          tokenRoles: this.token?.roles, // Roles del token (de todas las compañías)
        },
      );

      // ✅ Esperar a que los roles se carguen ANTES de cargar el menú
      // Esto asegura que MenuFilterService tenga los roles disponibles cuando filtre los menús
      this.userRoleService
        .getRolesByCompany(company.idCompany, false)
        .subscribe({
          next: (roles) => {
            this.logger.debug(
              `[Welcome] Roles cargados para compañía ${company.idCompany}:`,
              {
                companyName: company.name,
                rolesFromBackend: roles,
                rolesFromToken: this.token?.roles,
              },
            );

            // ✅ Verificar que los roles estén disponibles síncronamente antes de cargar el menú
            // Esto asegura que MenuFilterService pueda obtenerlos con getCurrentRolesSync()
            const rolesSync = this.userRoleService.getCurrentRolesSync();

            if (rolesSync.length > 0) {
              if (this.token?.userName) {
                this.loadMenuAndSetCompany(company, true);
              }
            } else {
              // Si aún no están en caché, esperar un momento más
              setTimeout(() => {
                const rolesSyncAfterWait =
                  this.userRoleService.getCurrentRolesSync();
                if (this.token?.userName) {
                  this.loadMenuAndSetCompany(company, true);
                }
              }, 200);
            }
          },
          error: (error) => {
            this.logger.error(
              '[Welcome] Error al cargar roles del usuario:',
              error,
            );

            // ✅ Aún así, intentar cargar el menú si hay userName
            // El MenuFilterService manejará el caso de roles vacíos
            if (this.token?.userName) {
              this.loadMenuAndSetCompany(company, true);
            }
          },
        });

      // 🔄 Refrescar suscripción después de seleccionar compañía
      // Esto asegura que los permisos de planes se validen correctamente
      this.logger.debug(
        '[Welcome] Refrescando suscripción después de seleccionar compañía:',
        {
          companyId: company.idCompany,
          companyName: company.name,
        },
      );
      this.permissionService.refreshSubscription();

      // 🔄 Invalidar TODO el caché de permisos después de seleccionar compañía
      // Esto asegura que los permisos se validen contra el backend para la compañía correcta
      // SIN usar caché, validando directamente contra el backend
      this.logger.debug(
        '[Welcome] Invalidando TODO el caché de permisos para la nueva compañía:',
        {
          companyId: company.idCompany,
          companyName: company.name,
        },
      );
      this.permissionService.invalidateAllCaches();

      // Esperar a que la suscripción se cargue antes de continuar
      // Esto asegura que el MenuFilterService pueda filtrar correctamente
      this.permissionService
        .getSubscription()
        .pipe(
          filter((sub: CompanySubscription | null) => sub !== null), // Esperar hasta que haya una suscripción cargada
          takeUntil(this.destroy$),
        )
        .subscribe({
          next: (subscription: CompanySubscription | null) => {
            if (subscription) {
              this.logger.debug(
                '[Welcome] Suscripción cargada después de seleccionar compañía:',
                {
                  companyId: company.idCompany,
                  planCode: subscription.plan?.code,
                  status: subscription.status,
                },
              );
            }
          },
          error: (err) => {
            this.logger.warn(
              '[Welcome] Error al cargar suscripción:',
              err,
            );
          },
        });

      // Guardar contactos de la compañía para usarlos en facturación
      if (company.contacts && company.contacts.length > 0) {
        localStorage.setItem(
          'COMPANY_CONTACTS',
          JSON.stringify(company.contacts),
        );
      }

      this.companyService.companyNameChange.next(company.name || '');
      this.companyService.companyChange.next(company);

      // ✅ CARGAR MENÚS INMEDIATAMENTE después de seleccionar la compañía
      // Los permisos son a nivel de compañía, no de sucursal o punto de emisión
      // No esperar a la selección de punto de emisión para cargar menús
      if (this.token?.userName) {
        // Cargar roles primero, luego menús
        this.userRoleService
          .getRolesByCompany(company.idCompany, false)
          .subscribe({
            next: (roles) => {
              this.loadMenuAndSetCompany(company, true);
            },
            error: (error) => {
              this.logger.error(
                '[Welcome] Error al cargar roles, cargando menús de todas formas:',
                error,
              );
              this.loadMenuAndSetCompany(company, true);
            },
          });
      }

      // Continuar con el flujo de puntos de emisión en paralelo (no bloquea la carga de menús)
      this.invoicePointService
        .getCurrentSession(currentUser)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (currentPoint) => {
            if (currentPoint) {
              this.continueLoginWithCurrentPoint(
                currentPoint,
                company,
                currentUser,
              );
            } else if (idEstablishment) {
              this.loadAvailablePoints(idEstablishment, company);
            } else {
              // Si no hay establecimiento seleccionado, no cargar puntos de emisión
              // pero los menús ya se cargaron arriba
              this.isLoading = false;
              this.isWait = false;
            }
          },
          error: (err) => {
            this.logger.error('Error al obtener sesión actual:', err);
            // No mostrar error al usuario, solo loguear
            // Los menús ya se cargaron arriba
            this.isLoading = false;
            this.isWait = false;
          },
        });

      this.isWait = true;
    });
  }

  assignInvoicePoint(
    selectedPoint: InvoiceIssuancePoint,
    company: Company,
  ): void {
    // Guardar datos en localStorage ANTES de asignar
    // (el servicio de sesión los necesita para iniciar el heartbeat)
    localStorage.setItem(
      environment.ID_INVOICE_POINT,
      selectedPoint.idInvoiceIssuancePoint.toString(),
    );

    const invoicePointData = {
      id: selectedPoint.idInvoiceIssuancePoint,
      code: selectedPoint.code,
      establishmentId: selectedPoint.establishmentId,
    };

    localStorage.setItem(
      'INVOICE_POINT_DATA',
      JSON.stringify(invoicePointData),
    );

    // Usar el servicio de sesión que maneja asignación + heartbeat automáticamente
    this.invoicePointSessionService
      .assignInvoicePointWithHeartbeat(selectedPoint.idInvoiceIssuancePoint)
      .then(() => {
        const token = decodeToken();
        this.loadMenuAndSetCompany(company, true);
      })
      .catch((err) => {
        this.logger.error('❌ Error al asignar punto de emisión', err);
        this.toast.httpError(
          err,
          'asignar punto de emisión',
          'Error al asignar punto de emisión. Intente nuevamente.',
        );
        this.isLoading = false;
        this.isWait = false;
      });
  }

  openPuntoDialog(points: InvoiceIssuancePoint[], company: Company): void {
    const activePoints = points.filter((p) => p.status);

    const dialogRef = this.dialog.open(SelectInvoicePointDialogComponent, {
      width: '500px',
      data: { points: activePoints },
      disableClose: false, // Permitir cerrar con ESC o click fuera
      hasBackdrop: true,
      backdropClass: 'invoice-point-dialog-backdrop',
      panelClass: 'invoice-point-dialog-panel',
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((selectedPoint: InvoiceIssuancePoint | undefined) => {
        if (selectedPoint) {
          this.assignInvoicePoint(selectedPoint, company);
        } else {
          // Usuario canceló, volver a la selección de compañía
          this.logger.debug(
            '❌ [WELCOME] Usuario canceló selección de punto, volviendo a compañías',
          );
          // Limpiar datos guardados temporalmente
          localStorage.removeItem(environment.ID_COMPANY);
          localStorage.removeItem(environment.IDENTIFICATION);
          localStorage.removeItem(environment.NAME_COMPANY);
          localStorage.removeItem(environment.ID_ESTABLISHMENT);
          localStorage.removeItem('COMPANY_CONTACTS');

          // Volver a cargar el diálogo de compañías
          this.loadDataCompanies();
        }
      });
  }

  private continueLoginWithCurrentPoint(
    currentPoint: InvoiceIssuancePoint,
    company: Company,
    userName: string,
  ): void {
    // Guardar ID del punto (para compatibilidad)
    localStorage.setItem(
      environment.ID_INVOICE_POINT,
      currentPoint.idInvoiceIssuancePoint.toString(),
    );

    // Guardar objeto completo del punto de emisión
    localStorage.setItem(
      'INVOICE_POINT_DATA',
      JSON.stringify({
        id: currentPoint.idInvoiceIssuancePoint,
        code: currentPoint.code,
        establishmentId: currentPoint.establishmentId,
      }),
    );

    // Iniciar heartbeat para la sesión existente
    this.invoicePointSessionService.startHeartbeat();
    this.loadMenuAndSetCompany(company);
  }

  private loadAvailablePoints(idEstablishment: number, company: Company): void {
    this.invoicePointService
      .findAvailableByEstablishments([idEstablishment])
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (points) => {
          if (points.length === 1) {
            this.assignInvoicePoint(points[0], company);
          } else if (points.length > 1) {
            this.openPuntoDialog(points, company);
          } else {
            // Cero puntos: puede ser por suscripción cancelada/inactiva o porque la sucursal no tiene puntos
            this.logger.warn(
              '⚠️ [Welcome] No hay puntos de emisión para esta sucursal; comprobando si hay suscripción activa',
            );
            this.companyService.setCompanyDialogOpenedWithWarning(true);

            // Limpiar datos de la selección que no tuvo puntos disponibles
            localStorage.removeItem(environment.ID_COMPANY);
            localStorage.removeItem(environment.IDENTIFICATION);
            localStorage.removeItem(environment.NAME_COMPANY);
            localStorage.removeItem(environment.ID_ESTABLISHMENT);
            localStorage.removeItem(environment.ID_INVOICE_POINT);
            localStorage.removeItem('INVOICE_POINT_DATA');

            this.permissionService
              .getSubscriptionForCompany(company.idCompany)
              .pipe(take(1), takeUntil(this.destroy$))
              .subscribe({
                next: (subscription) => {
                  const hasActiveSubscription =
                    subscription &&
                    (subscription.status === 'ACTIVE' ||
                      subscription.status === 'TRIAL');
                  const warningMessage = hasActiveSubscription
                    ? 'No hay puntos de emisión disponibles para esta sucursal. Por favor seleccione otra sucursal.'
                    : 'No hay suscripción activa para esta compañía. Por favor contacte al administrador o seleccione otra compañía.';
                  if (!hasActiveSubscription) {
                    this.logger.warn(
                      '[Welcome] Sin suscripción activa para la compañía, mostrando mensaje de suscripción',
                      {
                        companyId: company.idCompany,
                        status: subscription?.status ?? 'no encontrada',
                      },
                    );
                  }
                  setTimeout(() => {
                    this.openDialog(this.availableCompanies, warningMessage);
                  }, 100);
                },
                error: () => {
                  // Si falla la consulta de suscripción, asumir que no hay suscripción activa
                  const warningMessage =
                    'No hay suscripción activa para esta compañía. Por favor contacte al administrador o seleccione otra compañía.';
                  setTimeout(() => {
                    this.openDialog(this.availableCompanies, warningMessage);
                  }, 100);
                },
              });
          }
        },
        error: (err) => {
          this.logger.error('Error al cargar puntos de emisión:', err);

          // Limpiar datos de la selección que tuvo error
          localStorage.removeItem(environment.ID_COMPANY);
          localStorage.removeItem(environment.IDENTIFICATION);
          localStorage.removeItem(environment.NAME_COMPANY);
          localStorage.removeItem(environment.ID_ESTABLISHMENT);
          localStorage.removeItem(environment.ID_INVOICE_POINT);
          localStorage.removeItem('INVOICE_POINT_DATA');

          // Reabrir el diálogo con mensaje de error
          this.companyService.setCompanyDialogOpenedWithWarning(true);
          const errorMessage =
            'Error al cargar puntos de emisión. Por favor seleccione otra sucursal.';
          setTimeout(() => {
            this.openDialog(this.availableCompanies, errorMessage);
          }, 100);
        },
      });
  }

  /**
   * Carga los menús del usuario independientemente de la selección de compañía/establecimiento.
   * Los menús dependen solo del usuario autenticado.
   */
  private loadMenusForUser(): void {
    if (!this.token?.userName) {
      return;
    }

    this.menuService
      .findMenuByUser(this.token.userName)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (menu) => {
          if (menu && menu.length > 0) {
            this.logger.debug(
              `[Welcome] Menús cargados independientemente: ${menu.length} grupos`,
            );
            this.menuService.menuChange.next(menu);
            this.menuService.isLoginChange.next(true);
          }
        },
        error: (err) => {
          this.logger.error('[Welcome] Error al cargar menús del usuario:', err);
        },
      });
  }

  /**
   * Método centralizado para cargar el menú y establecer la compañía
   * Evita duplicación de código y facilita el mantenimiento
   * @param company Compañía a establecer (opcional, si no se proporciona usa la del localStorage)
   * @param openSidenav Si es true, emite el trigger para abrir el sidenav automáticamente
   */
  private loadMenuAndSetCompany(
    company?: Company,
    openSidenav: boolean = false,
  ): void {
    const token = decodeToken();

    if (!token?.userName) {
      this.logger.error(
        '[Welcome] No hay userName en token, abortando carga de menús',
      );
      this.isLoading = false;
      this.isWait = false;
      return;
    }

    this.menuService
      .findMenuByUser(token.userName)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (menu) => {
          // Verificar si menu es null o undefined
          if (!menu) {
            this.logger.error('[Welcome] menu es null o undefined');
            this.isLoading = false;
            this.isWait = false;
            return;
          }

          // Verificar si menu es un array vacío
          if (menu.length === 0) {
            this.logger.warn('[Welcome] menu es un array vacío');
          }

          this.logger.debug('📋 [Welcome] Menús recibidos del backend:', {
            totalMenus: menu?.length || 0,
            menus: menu?.map((m) => ({
              id: m.id,
              name: m.name,
              url: m.url,
              icon: m.icon,
              featureCode: m.featureCode,
              subMenusCount: m.subMenus?.length || 0,
              subMenus: m.subMenus?.map((sm) => ({
                id: sm.id,
                name: sm.name,
                url: sm.url,
                icon: sm.icon,
                featureCode: sm.featureCode,
              })),
            })),
            fullMenus: menu,
          });

          this.menuService.menuChange.next(menu);
          this.menuService.isLoginChange.next(true);

          if (company) {
            this.company = company;
            this.companyName = company.name;
            this.companyService.companyNameChange.next(company.name || '');
          } else {
            // Usar datos del localStorage si no se proporciona compañía
            const companyName = localStorage.getItem(environment.NAME_COMPANY);
            this.companyName = companyName || '';
            if (companyName) {
              this.companyService.companyNameChange.next(companyName || '');
            }
          }

          this.isLoading = false;
          this.isWait = false;

          // Cargar métricas después de establecer la compañía
          this.loadMetrics();

          // Emitir trigger para abrir el sidenav automáticamente si se solicita
          if (openSidenav) {
            setTimeout(() => {
              if (!this.destroy$.closed) {
                this.menuService.openSidenavTrigger.next();
                this.logger.debug('🔔 Trigger emitido para abrir menú lateral');
              }
            }, 500);
          }
        },
        error: (err) => {
          this.logger.error('Error al cargar menú:', err);
          this.isLoading = false;
          this.isWait = false;
        },
      });
  }

  /**
   * Carga las métricas (KPIs) de la pantalla Welcome
   * Usa el nuevo endpoint /api/dashboard/welcome
   */
  private loadMetrics(): void {
    // ⭐ Guard: Evitar llamadas duplicadas
    if (this.metricsLoaded || this.isLoadingMetrics) {
      this.logger.debug(
        '⏭️ [Welcome] Métricas ya cargadas o en proceso, omitiendo llamada duplicada',
      );
      return;
    }

    const companyId = localStorage.getItem(environment.ID_COMPANY);
    if (!companyId) {
      this.logger.warn('⚠️ [Welcome] No hay companyId para cargar métricas');
      return;
    }

    this.isLoadingMetrics = true;

    this.logger.debug(
      '📊 [Welcome] Cargando dashboard con companyId:',
      companyId,
    );

    this.dashboardService
      .getWelcomeDashboard(Number(companyId))
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isLoadingMetrics = false;
        }),
      )
      .subscribe({
        next: (response) => {
          const data = response.data;

          // Asignar KPIs desde el nuevo endpoint
          this.metrics.today = data.kpis.todaySales.amount || 0;
          this.metrics.week = data.kpis.weekSales.amount || 0;
          this.metrics.month = data.kpis.monthSales.amount || 0;
          this.metrics.pending = data.kpis.pendingCollection.amount || 0;
          this.metrics.pendingCount =
            data.kpis.pendingCollection.invoiceCount || 0;

          // Asignar datos de Fase 2
          this.salesTrendData = data.salesTrend || [];
          this.recentInvoices = (data.recentInvoices || []).slice(0, 5);

          // Marcar como cargadas
          this.metricsLoaded = true;

          // ⭐ Forzar detección de cambios para renderizar el canvas
          this.cdr.detectChanges();

          // ⭐ Inicializar gráfico después de que el canvas se renderice
          setTimeout(() => {
            if (this.salesChartRef && this.salesTrendData.length > 0) {
              this.initSalesChart();
            }
          }, 300); // Timeout más largo para asegurar que el canvas se renderice

          this.logger.debug('✅ [Welcome] Dashboard cargado:', {
            today: this.metrics.today,
            week: this.metrics.week,
            month: this.metrics.month,
            pending: this.metrics.pending,
            pendingCount: this.metrics.pendingCount,
            salesTrend: this.salesTrendData.length,
            recentInvoices: this.recentInvoices.length,
            alerts: data.alerts?.length || 0,
          });
        },
        error: (err) => {
          this.logger.error('❌ [Welcome] Error al cargar dashboard:', err);
          // No mostrar error al usuario, mantener en 0
          this.metrics = {
            today: 0,
            week: 0,
            month: 0,
            pending: 0,
            pendingCount: 0,
          };
          // No marcar como cargadas para permitir retry
        },
      });
  }

  /**
   * Carga el monto pendiente de cobro
   * DEPRECATED: Ya no se usa, ahora viene del endpoint /api/dashboard/welcome
   */
  private loadPendingAmount(): void {
    // Método deprecado - ahora se carga desde getWelcomeDashboard()
    this.logger.debug('⚠️ [Welcome] loadPendingAmount() deprecado');
  }

  /**
   * Formatea un número como moneda USD
   */
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-EC', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  }

  /**
   * Inicializa el gráfico de ventas (últimos 7 días)
   */
  private initSalesChart(): void {
    if (!this.salesChartRef || this.salesTrendData.length === 0) {
      this.logger.warn(
        '⚠️ [Welcome] No hay canvas o datos para inicializar el gráfico',
      );
      return;
    }

    // Destruir gráfico anterior si existe
    if (this.salesChart) {
      this.salesChart.destroy();
    }

    const canvas = this.salesChartRef.nativeElement;
    const ctx = canvas.getContext('2d');

    if (!ctx) {
      this.logger.error('❌ [Welcome] No se pudo obtener contexto del canvas');
      return;
    }

    // Preparar datos
    // ⭐ Invertir orden: del más antiguo al más nuevo (izquierda → derecha)
    const reversedData = [...this.salesTrendData].reverse();

    const labels = reversedData.map((item) => {
      const date = new Date(item.date);
      const day = date.getDate();
      const weekday = date.toLocaleDateString('es-EC', { weekday: 'short' });
      return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)} ${day}`;
    });

    const amounts = reversedData.map((item) => item.amount || 0);

    // Configurar gráfico
    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Ventas',
            data: amounts,
            borderColor: '#003a75',
            backgroundColor: 'rgba(0, 58, 117, 0.08)',
            borderWidth: 2,
            fill: true,
            tension: 0.3,
            pointRadius: 4,
            pointBackgroundColor: '#003a75',
            pointBorderColor: '#ffffff',
            pointBorderWidth: 2,
            pointHoverRadius: 6,
            pointHoverBackgroundColor: '#00acc1',
            pointHoverBorderColor: '#ffffff',
            pointHoverBorderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        layout: {
          padding: {
            left: 10,
            right: 20,
            top: 10,
            bottom: 10,
          },
        },
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            display: false,
          },
          tooltip: {
            backgroundColor: 'rgba(0, 58, 117, 0.95)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#003a75',
            borderWidth: 1,
            padding: 12,
            displayColors: false,
            titleFont: { size: 13, weight: 600 },
            bodyFont: { size: 12 },
            callbacks: {
              label: (context) => {
                const value = context.parsed.y ?? 0;
                const index = context.dataIndex;
                const invoiceCount = reversedData[index]?.invoiceCount || 0;
                return [
                  `Ventas: ${this.formatCurrency(value)}`,
                  `Facturas: ${invoiceCount}`,
                ];
              },
            },
          },
        },
        scales: {
          y: {
            beginAtZero: true,
            grace: '5%',
            ticks: {
              color: '#757575',
              font: { size: 11 },
              maxTicksLimit: 5,
              padding: 8,
              callback: (value) => {
                const num = Number(value);
                if (num === 0) return '$0';
                if (num >= 1000) {
                  return `$${(num / 1000).toFixed(1)}k`;
                }
                return `$${num.toFixed(0)}`;
              },
            },
            grid: {
              color: 'rgba(0, 0, 0, 0.06)',
            },
            border: {
              display: false,
            },
          },
          x: {
            ticks: {
              color: '#757575',
              font: { size: 11 },
              padding: 8,
            },
            grid: {
              display: false,
            },
          },
        },
        onClick: (event, elements) => {
          if (elements.length > 0) {
            const index = elements[0].index;
            const dataPoint = reversedData[index];
            this.onChartPointClick(dataPoint);
          }
        },
      },
    };

    this.salesChart = new Chart(ctx, config);
    this.logger.debug('✅ [Welcome] Gráfico de ventas inicializado');
  }

  /**
   * Maneja el click en un punto del gráfico
   */
  private onChartPointClick(dataPoint: any): void {
    const date = new Date(dataPoint.date);
    const formattedDate = date.toLocaleDateString('es-EC', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });

    const message = `📊 ${formattedDate}\n💰 Ventas: ${this.formatCurrency(
      dataPoint.amount || 0,
    )}\n📄 Facturas: ${dataPoint.invoiceCount || 0}`;

    this.toast.info(message);

    this.logger.debug('📊 [Welcome] Click en punto del gráfico:', dataPoint);
  }

  /**
   * Formatea una fecha para mostrar
   */
  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('es-EC', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  /**
   * Obtiene la clase CSS para el estado de autorización SRI
   */
  getAuthorizationClass(status: string): string {
    if (!status) return 'status-inactive';
    const statusUpper = status.toUpperCase();
    switch (statusUpper) {
      case 'AUTORIZADO':
      case 'AUTORIZADA':
        return 'status-success';
      case 'RECHAZADO':
      case 'RECHAZADA':
        return 'status-error';
      case 'EN_PROCESO':
      case 'PROCESANDO':
        return 'status-warning';
      case 'PENDIENTE_ENVIO':
      case 'PENDIENTE':
        return 'status-info';
      default:
        return 'status-inactive';
    }
  }

  /**
   * Obtiene el texto legible para el estado de autorización SRI
   */
  getAuthorizationLabel(status: string): string {
    if (!status) return 'N/A';
    const statusUpper = status.toUpperCase();
    switch (statusUpper) {
      case 'AUTORIZADO':
      case 'AUTORIZADA':
        return 'Autorizada';
      case 'RECHAZADO':
      case 'RECHAZADA':
        return 'Rechazada';
      case 'EN_PROCESO':
      case 'PROCESANDO':
        return 'En Proceso';
      case 'PENDIENTE_ENVIO':
      case 'PENDIENTE':
        return 'Pendiente';
      default:
        return status;
    }
  }

  /**
   * Navega al detalle de una factura
   */
  viewInvoiceDetail(invoice: any): void {
    if (invoice.idInvoice) {
      this.router.navigate(['/billing/invoices', invoice.idInvoice]);
    }
  }

  /**
   * Lifecycle hook - Limpia todas las suscripciones para evitar memory leaks
   */
  ngAfterViewInit(): void {
    // Esperar a que los datos estén cargados para inicializar el gráfico
    if (this.salesTrendData.length > 0) {
      setTimeout(() => this.initSalesChart(), 100);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    // Destruir gráfico si existe
    if (this.salesChart) {
      this.salesChart.destroy();
    }
  }
}
