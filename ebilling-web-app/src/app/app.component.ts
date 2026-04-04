import {
  ChangeDetectorRef,
  Component,
  ViewChild,
  QueryList,
  ViewChildren,
  AfterViewInit,
  OnDestroy,
} from '@angular/core';
import { Menu } from './_model/auth/menu';
import { MenuService } from './_service/auth/menu.service';
import { LoginService } from './_service/auth/login.service';
import { CompanyService } from './_service/customer/company.service';
import { MatDialog } from '@angular/material/dialog';
import { CompanyInfoComponent } from './pages/company/company-info/company-info.component';
import { decodeToken, isTokenValid } from './_functions/functions';
import { environment } from '../environments/environment';
import { JwtHelperService } from '@auth0/angular-jwt';
import { Company } from './_model/customer/company-establishment';
import { CompanyFilter } from './_model/customer/company-filter';
import { LoggerService } from './_service/logger/logger.service';
import { PaymentMethodService } from './_service/invoice/payment-method.service';
import { Router, NavigationEnd } from '@angular/router';
import { filter, takeUntil } from 'rxjs/operators';
import { MatSidenav } from '@angular/material/sidenav';
import { MatExpansionPanel } from '@angular/material/expansion';
import { Subject } from 'rxjs';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map, switchMap } from 'rxjs/operators';
import { MenuFilterService } from './_service/menu/menu-filter.service';
import { UserRoleService } from './_service/auth/user-role.service';
import { CompanyDialogComponent } from './pages/welcome/company-dialog/company-dialog.component';
import { PermissionService } from './_service/permission/permission.service';
import { InvoicePointSessionService } from './_service/auth/invoice-point-session.service';
import { InvoicePointService } from './_service/auth/invoice-point.service';
import { InvoiceIssuancePoint } from './_model/customer/invoice-issuance-point';
import { SelectInvoicePointDialogComponent } from './pages/welcome/select-invoice-point-dialog/select-invoice-point-dialog.component';
import { ToastService } from './_service/notification/toast.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements AfterViewInit, OnDestroy {
  title = 'ebilling-web-app';
  companyName = '';
  companies: Company[];

  menus: Menu[] = [];
  isLogin = false;
  canShowConfigMenu = false;
  hasMultipleCompaniesFlag = false;

  // Referencia al sidenav para poder abrirlo/cerrarlo programáticamente
  @ViewChild('sidenav') sidenav!: MatSidenav;

  // Referencias a los expansion panels para auto-expandir el activo
  @ViewChildren(MatExpansionPanel)
  expansionPanels!: QueryList<MatExpansionPanel>;

  // Subject para limpiar suscripciones y evitar memory leaks
  private readonly destroy$ = new Subject<void>();

  // Timeout para expandActivePanel (para poder cancelarlo si es necesario)
  private expandPanelTimeout: ReturnType<typeof setTimeout> | null = null;

  // Rutas públicas que NO deben mostrar el menú ni toolbar
  private readonly publicRoutes = [
    '/login',
    '/reset-password',
    '/token-validation', // Redirección alternativa para reset de contraseña
    '/invoice-consultation',
    '/viewing-invoices',
    '/not-403',
    '/not-404',
    '/invoices/ticket', // ✅ Ruta de impresión de tirilla - sin layout
    '/invoices/debit-note-ticket', // ✅ Ruta de impresión de tirilla de nota de débito - sin layout
    '/delivery-guides/ticket', // ✅ Ruta de impresión de ticket de guía de remisión - sin layout
  ];

  // Controla si debemos mostrar el layout completo (toolbar + sidenav)
  showLayout = false;

  // Responsive: control del sidenav
  isHandset = false;
  sidenavMode: 'side' | 'over' = 'side';
  sidenavOpened = true;

  // ✅ Logo dinámico de la compañía (con fallback a Wallparisoft)
  companyLogoUrl: string | null = null;
  readonly fallbackLogoUrl = 'assets/logo-wallparisoft.png'; // ✅ Logo sin texto para sidebar
  readonly toolbarLogoUrl = 'assets/logo-wallparisoft-texto.png'; // ✅ Logo con texto para header

  constructor(
    private readonly menuService: MenuService,
    private readonly companyService: CompanyService,
    private jwtHelper: JwtHelperService,
    public loginService: LoginService,
    private readonly dialog: MatDialog,
    private readonly cdRef: ChangeDetectorRef,
    private readonly logger: LoggerService,
    private readonly paymentMethodService: PaymentMethodService,
    private readonly router: Router,
    private readonly breakpointObserver: BreakpointObserver,
    private readonly menuFilterService: MenuFilterService,
    private readonly userRoleService: UserRoleService,
    private readonly permissionService: PermissionService,
    private readonly invoicePointSessionService: InvoicePointSessionService,
    private readonly invoicePointService: InvoicePointService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.logger.debug('AppComponent initialized');

    // Inicializar flag de múltiples empresas
    this.hasMultipleCompaniesFlag = this.hasMultipleCompanies();

    // Configurar responsive sidenav
    this.setupResponsiveSidenav();

    // Pre-cargar formas de pago al iniciar la aplicación
    // Esto inicializa el caché para usarse en el módulo de facturación
    this.preloadPaymentMethods();

    // Detectar cambios de ruta para ocultar/mostrar el layout y auto-expandir panel activo
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntil(this.destroy$),
      )
      .subscribe((event: NavigationEnd) => {
        this.updateLayoutVisibility(event.urlAfterRedirects);
        // Auto-expandir el panel que contiene la ruta activa
        this.scheduleExpandActivePanel(event.urlAfterRedirects, 100);
        // Cerrar sidenav en mobile después de navegar
        this.closeSidenavOnMobile();
      });

    // Inicializar la visibilidad del layout con la ruta actual
    this.updateLayoutVisibility(this.router.url);

    this.menuService.menuChange
      .pipe(
        switchMap((data) => {
          // Filtrar menús por permisos de suscripción
          return this.menuFilterService.filterMenus(data);
        }),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (filteredMenus) => {
          // Verificar si filteredMenus es null o undefined
          if (!filteredMenus) {
            this.logger.error(
              '[AppComponent] filteredMenus es null o undefined',
            );
            this.menus = [];
            this.cdRef.detectChanges();
            return;
          }

          this.menus = filteredMenus;
          this.logger.debug(
            `Menús filtrados por permisos: ${filteredMenus.length} grupos`,
          );
          this.cdRef.detectChanges();
          // Auto-expandir panel activo después de cargar los menús
          this.scheduleExpandActivePanel(this.router.url, 200);
        },
        error: (error) => {
          this.logger.error('[AppComponent] Error al filtrar menús:', error);
          this.menus = [];
          this.cdRef.detectChanges();
        },
      });

    // Suscribirse al trigger para abrir el sidenav
    this.menuService.openSidenavTrigger
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        setTimeout(() => {
          if (this.sidenav && !this.destroy$.closed) {
            this.sidenav.open();
            this.logger.debug(
              '✅ Menú lateral abierto automáticamente desde trigger',
            );
          }
        }, 300);
      });

    this.companyService.companyNameChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        this.companyName = data || '';
        this.cdRef.detectChanges();
      });

    // ✅ Suscribirse a cambios de compañía para cargar el logo dinámico
    this.companyService.companyChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((company) => {
        this.logger.debug('🔍 [AppComponent] Company change:', {
          idCompany: company?.idCompany,
          logoPath: (company as any)?.logoPath,
          name: company?.name,
        });
        this.loadCompanyLogo(company?.idCompany, (company as any)?.logoPath);
      });

    // ✅ Cargar logo inicial si ya hay compañía seleccionada
    const storedCompanyId = localStorage.getItem(environment.ID_COMPANY);
    const storedLogoPath = localStorage.getItem('COMPANY_LOGO_PATH');
    if (storedCompanyId) {
      this.loadCompanyLogo(
        Number(storedCompanyId),
        storedLogoPath || undefined,
      );
    }
    // Sincronizar ambas fuentes de verdad para isLogin
    // Priorizar loginState$ como fuente principal, pero también escuchar isLoginChange
    this.loginService.loginState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((state) => {
        this.isLogin = state;
        this.canShowConfigMenu = this.showConfigMenu();
        // Actualizar flag de múltiples empresas cuando cambia el estado de login
        this.hasMultipleCompaniesFlag = this.hasMultipleCompanies();
        this.updateLayoutVisibility(this.router.url);
        this.cdRef.detectChanges();

        // Pre-cargar formas de pago cuando el usuario hace login
        if (state) {
          this.preloadPaymentMethods();
        }
      });

    // Suscribirse a cambios en los roles para actualizar canShowConfigMenu
    this.userRoleService.currentRoles
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.canShowConfigMenu = this.showConfigMenu();
        this.cdRef.detectChanges();
      });

    // Escuchar cambios del menú y sincronizar con loginState
    // Esto asegura que cuando el menú se carga, también se establece el estado de login
    this.menuService.isLoginChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((data) => {
        // Solo actualizar si el estado es diferente y es true
        // Si es false, mantener el estado actual de loginState$
        if (data && !this.isLogin) {
          this.isLogin = true;
          this.loginService.setLoginStatus(true);
          // Actualizar flag de múltiples empresas cuando se carga el menú
          this.hasMultipleCompaniesFlag = this.hasMultipleCompanies();
          this.updateLayoutVisibility(this.router.url);
          this.cdRef.detectChanges();
        } else if (!data && this.isLogin) {
          // Si el menú se desactiva, también desactivar login
          this.isLogin = false;
          this.loginService.setLoginStatus(false);
          this.hasMultipleCompaniesFlag = false;
          this.updateLayoutVisibility(this.router.url);
          this.cdRef.detectChanges();
        }
      });
  }

  /**
   * Configura el sidenav responsive usando BreakpointObserver
   * Desktop (>= 960px): mode="side", opened=true
   * Mobile (< 960px): mode="over", opened=false
   */
  private setupResponsiveSidenav(): void {
    this.breakpointObserver
      .observe(['(max-width: 959px)'])
      .pipe(
        map((result) => result.matches),
        takeUntil(this.destroy$),
      )
      .subscribe((isHandset) => {
        this.isHandset = isHandset;
        if (isHandset) {
          // Mobile: overlay mode, cerrado por defecto
          this.sidenavMode = 'over';
          this.sidenavOpened = false;
        } else {
          // Desktop: side mode, abierto por defecto
          this.sidenavMode = 'side';
          this.sidenavOpened = true;
        }
        this.cdRef.detectChanges();
      });
  }

  /**
   * Cierra el sidenav en mobile después de navegar
   */
  closeSidenavOnMobile(): void {
    if (this.isHandset && this.sidenav) {
      this.sidenav.close();
    }
  }

  /**
   * Actualiza la visibilidad del layout (toolbar + sidenav)
   * El layout se muestra solo si:
   * 1. El usuario está logueado (isLogin = true)
   * 2. Y NO está en una ruta pública
   */
  private updateLayoutVisibility(url: string): void {
    const isPublicRoute = this.publicRoutes.some((route) =>
      url.startsWith(route),
    );
    this.showLayout = this.isLogin && !isPublicRoute;
    this.logger.debug(
      `Layout visibility - URL: ${url}, isLogin: ${this.isLogin}, isPublic: ${isPublicRoute}, showLayout: ${this.showLayout}`,
    );
  }

  /**
   * Pre-carga las formas de pago en el caché
   * Esto se ejecuta al iniciar la aplicación para tener los datos disponibles
   * sin demora cuando el usuario acceda al módulo de facturación
   */
  private preloadPaymentMethods(): void {
    // Solo pre-cargar si hay un token válido y no expirado
    if (!isTokenValid()) {
      this.logger.debug(
        'No se pre-cargan formas de pago (sin token válido o token expirado)',
      );
      return;
    }

    // Pre-cargar formas de pago en segundo plano
    this.paymentMethodService.findAll().subscribe({
      next: (methods) => {
        this.logger.debug(
          `✅ Formas de pago pre-cargadas: ${methods.length} métodos disponibles`,
        );
      },
      error: (err) => {
        // Solo loggear errores que no sean de conexión (status 0 = servicio no disponible)
        // y no mostrar errores 401 (token expirado) ya que ya validamos antes
        if (err.status !== 0 && err.status !== 401) {
          this.logger.warn('No se pudieron pre-cargar formas de pago', err);
        } else if (err.status === 401) {
          this.logger.debug(
            'Token expirado al pre-cargar formas de pago (se cargarán cuando sea necesario)',
          );
        } else {
          this.logger.debug(
            'Servicio de formas de pago no disponible (puede ser normal si el servicio no está corriendo)',
          );
        }
        // Las formas de pago se cargarán cuando sea necesario
      },
    });
  }

  /**
   * ✅ Carga el logo de la compañía seleccionada
   * Usa HttpClient para enviar el token JWT y crear un Blob URL
   * Si no hay logo o falla, usa el fallback de Wallparisoft
   */
  private loadCompanyLogo(
    idCompany: number | null | undefined,
    logoPath?: string,
  ): void {
    this.logger.debug('🔍 [loadCompanyLogo] Params:', { idCompany, logoPath });

    if (!idCompany) {
      this.companyLogoUrl = null;
      this.cdRef.detectChanges();
      return;
    }

    // ✅ Si hay logoPath, cargar imagen usando HttpClient (con token JWT)
    if (logoPath) {
      const logoUrl = this.companyService.getLogoUrl(idCompany);
      this.logger.debug('🖼️ Cargando logo desde endpoint con auth:', logoUrl);

      this.companyService.getLogoAsBlob(idCompany).subscribe({
        next: (blob) => {
          // Crear URL de objeto desde el blob
          const objectUrl = URL.createObjectURL(blob);
          this.companyLogoUrl = objectUrl;
          localStorage.setItem('COMPANY_HAS_LOGO', 'true');
          this.logger.debug(
            '✅ Logo de compañía cargado como Blob:',
            objectUrl,
          );
          this.cdRef.detectChanges();
        },
        error: (err) => {
          this.logger.warn(
            '⚠️ No se pudo cargar logo, usando fallback:',
            err.status,
          );
          this.companyLogoUrl = null;
          localStorage.removeItem('COMPANY_HAS_LOGO');
          this.cdRef.detectChanges();
        },
      });
      return;
    }

    // Sin logoPath, usar fallback
    this.companyLogoUrl = null;
    this.logger.debug('ℹ️ Compañía sin logoPath, usando fallback Wallparisoft');
    this.cdRef.detectChanges();
  }

  openDialog(companies: Company[]) {
    const dialogRef = this.dialog.open(CompanyInfoComponent, {
      width: '800px',
      disableClose: true,
      data: companies,
    });
    dialogRef.afterClosed().subscribe((result) => {
      localStorage.setItem(environment.ID_COMPANY, String(result.idCompany));
      localStorage.setItem(environment.IDENTIFICATION, result.identification);
      localStorage.setItem(environment.NAME_COMPANY, result.name);
      this.companyService.companyChange.next(result);
      this.companyService.companyNameChange.next(result.name || '');

      // Abrir el sidenav automáticamente después de seleccionar compañía y punto de emisión
      setTimeout(() => {
        if (this.sidenav) {
          this.sidenav.open();
          this.logger.debug('✅ Menú lateral abierto automáticamente');
        }
      }, 300); // Pequeño delay para asegurar que el sidenav esté disponible
    });
  }

  showInfoCompany() {
    let token = decodeToken();
    if (token.companies != null && token.companies.length > 0) {
      this.companyService.findByIds(token.companies).subscribe((data) => {
        if (data.code === 0) {
          this.openDialog(data.companyDetailsDtos);
        }
      });
    }
  }

  /**
   * Cambia la empresa actual sin cerrar sesión
   * Abre el diálogo de selección de empresa y actualiza todos los servicios necesarios
   */
  changeCompany(): void {
    const token = decodeToken();
    if (!token) {
      this.logger.warn('[AppComponent] No hay token disponible');
      return;
    }

    // Obtener los IDs de las empresas desde companyEstablishments o companies
    let companyIds: number[] = [];
    if (token.companies && Array.isArray(token.companies)) {
      companyIds = token.companies;
    } else if (
      (token as any).companyEstablishments &&
      Array.isArray((token as any).companyEstablishments)
    ) {
      companyIds = (token as any).companyEstablishments.map(
        (c: any) => c.idCompany,
      );
    } else if (
      (token as any).companyIds &&
      Array.isArray((token as any).companyIds)
    ) {
      companyIds = (token as any).companyIds;
    }

    if (!companyIds || companyIds.length === 0) {
      this.logger.warn(
        '[AppComponent] No hay empresas disponibles para cambiar',
      );
      return;
    }

    // Si solo tiene una empresa, no tiene sentido cambiar
    const uniqueCompanyIds = [...new Set(companyIds)];
    if (uniqueCompanyIds.length === 1) {
      this.logger.debug(
        '[AppComponent] Usuario solo tiene una empresa disponible',
      );
      return;
    }

    this.logger.debug(
      '[AppComponent] Abriendo diálogo para cambiar de empresa:',
      {
        companiesCount: uniqueCompanyIds.length,
        companyIds: uniqueCompanyIds,
        currentCompanyId: localStorage.getItem(environment.ID_COMPANY),
      },
    );

    // Obtener las empresas del usuario
    this.companyService.findByIds(uniqueCompanyIds).subscribe({
      next: (data) => {
        if (
          data.code === 0 &&
          data.companyDetailsDtos &&
          data.companyDetailsDtos.length > 0
        ) {
          // Cerrar el diálogo anterior si existe
          if (this.companyDialogRef) {
            this.companyDialogRef.close();
            this.companyDialogRef = null;
          }

          // Abrir el diálogo de selección de empresa
          this.companyDialogRef = this.dialog.open(CompanyDialogComponent, {
            width: '95vw',
            maxWidth: '650px',
            data: data.companyDetailsDtos,
            disableClose: false, // Permitir cerrar sin seleccionar
            hasBackdrop: true,
            backdropClass: 'company-dialog-backdrop',
            panelClass: 'company-dialog-panel',
          });

          this.companyDialogRef
            .afterClosed()
            .subscribe(
              (
                result:
                  | { company: Company; idEstablishment: number }
                  | null
                  | undefined,
              ) => {
                // Limpiar referencia al diálogo cuando se cierre
                this.companyDialogRef = null;
                if (!result) {
                  // Usuario canceló el cambio
                  this.logger.debug(
                    '[AppComponent] Cambio de empresa cancelado por el usuario',
                  );
                  return;
                }

                const { company, idEstablishment } = result;
                const currentCompanyId = localStorage.getItem(
                  environment.ID_COMPANY,
                );
                const currentEstablishmentId = localStorage.getItem(
                  environment.ID_ESTABLISHMENT,
                );

                // Verificar si cambió la empresa o el establecimiento
                const companyChanged =
                  currentCompanyId !== company.idCompany.toString();
                const establishmentChanged =
                  currentEstablishmentId !== idEstablishment?.toString();

                // Si no cambió ni la empresa ni el establecimiento, no hacer nada
                if (!companyChanged && !establishmentChanged) {
                  this.logger.debug(
                    '[AppComponent] Usuario seleccionó la misma empresa y sucursal, no hay cambios',
                  );
                  return;
                }

                // Si solo cambió el establecimiento (misma empresa), también debemos cambiar el punto de emisión
                if (!companyChanged && establishmentChanged) {
                  this.logger.debug(
                    '[AppComponent] Usuario cambió de sucursal dentro de la misma empresa, cambiando punto de emisión',
                    {
                      companyId: company.idCompany,
                      fromEstablishmentId: currentEstablishmentId,
                      toEstablishmentId: idEstablishment,
                    },
                  );
                }

                this.logger.debug('[AppComponent] Cambiando de empresa:', {
                  fromCompanyId: currentCompanyId,
                  toCompanyId: company.idCompany,
                  toCompanyName: company.name,
                  idEstablishment,
                  companyChanged,
                  establishmentChanged,
                });

                // No liberar el punto de emisión ahora. Se liberará solo cuando
                // se asigne uno nuevo, para evitar quedarse sin punto si el usuario
                // vuelve a la misma empresa/sucursal.
                this.proceedWithCompanyChange(
                  company,
                  idEstablishment,
                  data.companyDetailsDtos,
                );
              },
            );
        } else {
          this.logger.error(
            '[AppComponent] Error al obtener empresas del usuario:',
            data,
          );
        }
      },
      error: (error) => {
        this.logger.error(
          '[AppComponent] Error al cargar empresas para cambio:',
          error,
        );
      },
    });
  }

  // Guardar referencia a las empresas disponibles para poder reabrir el diálogo si es necesario
  private availableCompanies: Company[] = [];

  // Guardar la empresa original antes de cambiar (para poder revertir si no hay puntos)
  private originalCompanyBeforeChange: {
    idCompany: string | null;
    identification: string | null;
    name: string | null;
  } | null = null;

  // Referencia al diálogo de selección de empresa para evitar abrirlo múltiples veces
  private companyDialogRef: any = null;

  /**
   * Procede con el cambio de empresa después de liberar el punto de emisión
   * @private
   */
  private proceedWithCompanyChange(
    company: Company,
    idEstablishment?: number,
    companies?: Company[],
  ): void {
    // Guardar la empresa original ANTES de cambiar (para poder revertir si no hay puntos)
    this.originalCompanyBeforeChange = {
      idCompany: localStorage.getItem(environment.ID_COMPANY),
      identification: localStorage.getItem(environment.IDENTIFICATION),
      name: localStorage.getItem(environment.NAME_COMPANY),
    };

    // Guardar datos de la nueva compañía
    localStorage.setItem(environment.ID_COMPANY, company.idCompany.toString());
    localStorage.setItem(environment.IDENTIFICATION, company.identification);
    localStorage.setItem(environment.NAME_COMPANY, company.name);

    // Guardar establecimiento si se proporcionó
    if (idEstablishment) {
      localStorage.setItem(
        environment.ID_ESTABLISHMENT,
        idEstablishment.toString(),
      );
    } else {
      // Si no hay establecimiento, limpiarlo
      localStorage.removeItem(environment.ID_ESTABLISHMENT);
    }

    // No limpiar punto de emisión aquí. Se liberará y reasignará solo cuando
    // se seleccione un punto nuevo en assignInvoicePoint(). Esto evita perder
    // el punto actual si el usuario vuelve a la misma empresa/sucursal.

    // Limpiar caché de roles de la compañía anterior
    this.userRoleService.clearCache();

    // Actualizar servicios
    // Convertir Company a CompanyFilter para el Subject
    const companyFilter: CompanyFilter = {
      idCompany: company.idCompany,
      idType: company.idType,
      identification: company.identification,
      name: company.name,
      nameCommercial: company.nameCommercial,
      forcedToAccounting: company.forcedToAccounting,
      regime: company.regime,
      specialTaxpayerResolution: company.specialTaxpayerResolution,
      status: company.status,
      logoPath: (company as any)?.logoPath,
      contacts: company.contacts,
      principal: '', // Valor por defecto requerido por CompanyFilter
      isSuperAdmin: false, // Valor por defecto requerido por CompanyFilter
    };
    this.companyService.companyChange.next(companyFilter);
    this.companyService.companyNameChange.next(company.name || '');

    // Cargar roles del usuario en la nueva compañía
    this.userRoleService.getRolesByCompany(company.idCompany, false).subscribe({
      next: (roles) => {
        this.logger.debug(
          `[AppComponent] Roles cargados para nueva compañía ${company.idCompany}:`,
          roles,
        );
        // Actualizar canShowConfigMenu después de cargar roles
        this.canShowConfigMenu = this.showConfigMenu();

        // Recargar menús después de cambiar de compañía para que se filtren con los nuevos roles
        const token = decodeToken();
        if (token?.userName) {
          this.logger.debug(
            '[AppComponent] Recargando menús después de cambiar de compañía',
          );
          this.menuService.findMenuByUser(token.userName).subscribe({
            next: (menus) => {
              if (menus && menus.length > 0) {
                this.menuService.menuChange.next(menus);
                this.logger.debug(
                  `[AppComponent] Menús recargados: ${menus.length} grupos`,
                );
              }
            },
            error: (err) => {
              this.logger.error(
                '[AppComponent] Error al recargar menús después de cambiar de compañía:',
                err,
              );
            },
          });
        }

        this.cdRef.detectChanges();
      },
      error: (error) => {
        this.logger.error(
          '[AppComponent] Error al cargar roles de la nueva compañía:',
          error,
        );
        // Aún así actualizar canShowConfigMenu
        this.canShowConfigMenu = this.showConfigMenu();
        this.cdRef.detectChanges();
      },
    });

    // Refrescar suscripción para la nueva compañía
    this.permissionService.refreshSubscription();

    // Invalidar todo el caché de permisos
    this.permissionService.invalidateAllCaches();

    // Cargar logo de la nueva compañía
    this.loadCompanyLogo(company.idCompany, (company as any)?.logoPath);

    // Guardar contactos de la compañía si existen
    if (company.contacts && company.contacts.length > 0) {
      localStorage.setItem(
        'COMPANY_CONTACTS',
        JSON.stringify(company.contacts),
      );
    }

    // Los menús se recargarán automáticamente cuando MenuService detecte el cambio
    // a través de sus observables. No necesitamos recargarlos manualmente aquí.
    this.logger.debug(
      '[AppComponent] Cambio de empresa completado, los menús se recargarán automáticamente',
    );

    // Guardar empresas disponibles para poder reabrir el diálogo si es necesario
    if (companies) {
      this.availableCompanies = companies;
    }

    // Cargar puntos de emisión si hay un establecimiento seleccionado
    if (idEstablishment) {
      this.loadInvoicePointsForEstablishment(
        idEstablishment,
        company,
        companies,
      );
    } else {
      // Si no hay establecimiento, redirigir a welcome directamente
      this.router.navigate(['/welcome']).then(() => {
        this.logger.debug(
          '[AppComponent] Redirigido a welcome después de cambiar empresa',
        );
      });
    }
  }

  /**
   * Carga los puntos de emisión disponibles para un establecimiento y muestra el diálogo de selección
   * @private
   */
  private loadInvoicePointsForEstablishment(
    idEstablishment: number,
    company: Company,
    companies?: Company[],
  ): void {
    const token = decodeToken();
    const userName = token?.userName;

    if (!userName) {
      this.logger.warn(
        '[AppComponent] No hay userName en el token, no se pueden cargar puntos de emisión',
      );
      this.router.navigate(['/welcome']);
      return;
    }

    // Verificar si hay una sesión actual de punto de emisión
    this.invoicePointService.getCurrentSession(userName).subscribe({
      next: (currentPoint) => {
        if (currentPoint) {
          // Si hay una sesión actual y pertenece al establecimiento seleccionado, usarla
          if (currentPoint.establishmentId === idEstablishment) {
            this.logger.debug(
              '[AppComponent] Usando punto de emisión de sesión actual:',
              currentPoint,
            );
            // Guardar datos del punto de emisión
            localStorage.setItem(
              environment.ID_INVOICE_POINT,
              currentPoint.idInvoiceIssuancePoint.toString(),
            );
            const invoicePointData = {
              id: currentPoint.idInvoiceIssuancePoint,
              code: currentPoint.code,
              establishmentId: currentPoint.establishmentId,
            };
            localStorage.setItem(
              'INVOICE_POINT_DATA',
              JSON.stringify(invoicePointData),
            );
            // Redirigir a welcome
            this.router.navigate(['/welcome']);
          } else {
            // La sesión actual es de otro establecimiento, cargar puntos disponibles
            this.loadAvailableInvoicePoints(
              idEstablishment,
              company,
              companies,
            );
          }
        } else {
          // No hay sesión actual, cargar puntos disponibles
          this.loadAvailableInvoicePoints(idEstablishment, company, companies);
        }
      },
      error: (err) => {
        this.logger.error(
          '[AppComponent] Error al obtener sesión actual de punto de emisión:',
          err,
        );
        // Continuar cargando puntos disponibles aunque falle la verificación de sesión
        this.loadAvailableInvoicePoints(idEstablishment, company, companies);
      },
    });
  }

  /**
   * Vuelve a abrir el diálogo de selección de empresa/sucursal
   * Se usa cuando no hay puntos de emisión disponibles para la sucursal seleccionada
   * @param companies - Lista de empresas disponibles
   * @param warningMessage - Mensaje opcional a mostrar después de abrir el modal
   * @private
   */
  private reopenCompanyDialog(
    companies: Company[],
    warningMessage?: string,
  ): void {
    if (!companies || companies.length === 0) {
      this.logger.warn(
        '[AppComponent] No hay empresas disponibles para reabrir el diálogo',
      );
      // Si no hay empresas, redirigir a welcome
      this.router.navigate(['/welcome']);
      return;
    }

    this.logger.debug(
      '[AppComponent] Reabriendo diálogo de selección de empresa/sucursal (no hay puntos disponibles)',
    );

    // Revertir cambios en localStorage para permitir seleccionar otra sucursal/empresa
    // IMPORTANTE: Limpiar ID_COMPANY también para que WelcomeComponent muestre el diálogo
    // Si mantenemos ID_COMPANY, WelcomeComponent detectará que hay empresa y cargará el menú sin mostrar el diálogo
    localStorage.removeItem(environment.ID_COMPANY);
    localStorage.removeItem(environment.IDENTIFICATION);
    localStorage.removeItem(environment.NAME_COMPANY);
    localStorage.removeItem(environment.ID_ESTABLISHMENT);
    localStorage.removeItem(environment.ID_INVOICE_POINT);
    localStorage.removeItem('INVOICE_POINT_DATA');
    localStorage.removeItem('COMPANY_CONTACTS');

    // También limpiar los servicios para que no queden datos en memoria
    this.companyService.companyChange.next(null as any);
    this.companyService.companyNameChange.next('');

    // Cerrar el diálogo anterior si existe
    if (this.companyDialogRef) {
      this.companyDialogRef.close();
      this.companyDialogRef = null;
    }

    // Pequeño delay para asegurar que los cambios en localStorage se hayan aplicado
    // y que cualquier navegación pendiente se complete antes de abrir el diálogo
    setTimeout(() => {
      // Abrir el diálogo nuevamente con el mensaje de advertencia incluido en los datos
      this.companyDialogRef = this.dialog.open(CompanyDialogComponent, {
        width: '95vw',
        maxWidth: '650px',
        data: warningMessage ? { companies, warningMessage } : companies,
        disableClose: false,
        hasBackdrop: true,
        backdropClass: 'company-dialog-backdrop',
        panelClass: 'company-dialog-panel',
      });

      this.companyDialogRef
        .afterClosed()
        .subscribe(
          (
            result:
              | { company: Company; idEstablishment: number }
              | null
              | undefined,
          ) => {
            // Limpiar referencia al diálogo cuando se cierre
            this.companyDialogRef = null;
            if (!result) {
              // Usuario canceló - restaurar empresa original si existe
              if (this.originalCompanyBeforeChange) {
                if (this.originalCompanyBeforeChange.idCompany) {
                  localStorage.setItem(
                    environment.ID_COMPANY,
                    this.originalCompanyBeforeChange.idCompany,
                  );
                }
                if (this.originalCompanyBeforeChange.identification) {
                  localStorage.setItem(
                    environment.IDENTIFICATION,
                    this.originalCompanyBeforeChange.identification,
                  );
                }
                if (this.originalCompanyBeforeChange.name) {
                  localStorage.setItem(
                    environment.NAME_COMPANY,
                    this.originalCompanyBeforeChange.name,
                  );
                }
              }
              this.logger.debug(
                '[AppComponent] Usuario canceló selección de empresa/sucursal',
              );
              // Limpiar referencia a empresa original
              this.originalCompanyBeforeChange = null;
              return;
            }

            const { company, idEstablishment } = result;

            // Liberar punto de emisión actual antes de cambiar
            this.invoicePointSessionService
              .releaseInvoicePointWithHeartbeat()
              .then(() => {
                this.logger.debug(
                  '[AppComponent] ✅ Punto de emisión liberado correctamente',
                );
                // Continuar con el cambio de empresa después de liberar el punto
                this.proceedWithCompanyChange(
                  company,
                  idEstablishment,
                  companies,
                );
              })
              .catch((err) => {
                this.logger.warn(
                  '[AppComponent] ⚠️ Error al liberar punto de emisión, continuando con cambio de empresa:',
                  err,
                );
                this.proceedWithCompanyChange(
                  company,
                  idEstablishment,
                  companies,
                );
              });
          },
        );
    }, 100); // Pequeño delay para asegurar que los cambios se apliquen
  }

  /**
   * Carga los puntos de emisión disponibles y muestra el diálogo de selección
   * @private
   */
  private loadAvailableInvoicePoints(
    idEstablishment: number,
    company: Company,
    companies?: Company[],
  ): void {
    this.logger.debug(
      '[AppComponent] Cargando puntos de emisión disponibles para establecimiento:',
      idEstablishment,
    );

    this.invoicePointService
      .findAvailableByEstablishments([idEstablishment])
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (points) => {
          if (points.length === 1) {
            // Si hay un solo punto, asignarlo automáticamente
            this.logger.debug(
              '[AppComponent] Solo hay un punto de emisión disponible, asignándolo automáticamente',
            );
            this.assignInvoicePoint(points[0], company);
          } else if (points.length > 1) {
            // Si hay múltiples puntos, mostrar el diálogo de selección
            this.logger.debug(
              `[AppComponent] Hay ${points.length} puntos de emisión disponibles, mostrando diálogo de selección`,
            );
            this.openInvoicePointDialog(points, company);
          } else {
            // No hay puntos de emisión disponibles
            this.logger.warn(
              '[AppComponent] No hay puntos de emisión disponibles para este establecimiento',
            );
            // Volver a abrir el diálogo de selección de empresa/sucursal con mensaje de advertencia
            this.reopenCompanyDialog(
              companies || this.availableCompanies,
              'No hay puntos de emisión disponibles para esta sucursal. Por favor seleccione otra sucursal.',
            );
          }
        },
        error: (err) => {
          this.logger.error(
            '[AppComponent] Error al cargar puntos de emisión:',
            err,
          );
          // Volver a abrir el diálogo de selección de empresa/sucursal con mensaje de error
          const errorMessage =
            'Error al cargar puntos de emisión. Por favor seleccione otra sucursal.';
          this.reopenCompanyDialog(
            companies || this.availableCompanies,
            errorMessage,
          );
        },
      });
  }

  /**
   * Abre el diálogo de selección de puntos de emisión
   * @private
   */
  private openInvoicePointDialog(
    points: InvoiceIssuancePoint[],
    company: Company,
  ): void {
    const activePoints = points.filter((p) => p.status);

    const dialogRef = this.dialog.open(SelectInvoicePointDialogComponent, {
      width: '500px',
      data: { points: activePoints },
      disableClose: false,
      hasBackdrop: true,
      backdropClass: 'invoice-point-dialog-backdrop',
      panelClass: 'invoice-point-dialog-panel',
    });

    dialogRef
      .afterClosed()
      .subscribe((selectedPoint: InvoiceIssuancePoint | undefined) => {
        if (selectedPoint) {
          this.assignInvoicePoint(selectedPoint, company);
        } else {
          // Usuario canceló la selección
          this.logger.debug(
            '[AppComponent] Usuario canceló selección de punto de emisión',
          );
          // Redirigir a welcome de todas formas
          this.router.navigate(['/welcome']);
        }
      });
  }

  /**
   * Asigna un punto de emisión y redirige a welcome
   * @private
   */
  private assignInvoicePoint(
    selectedPoint: InvoiceIssuancePoint,
    company: Company,
  ): void {
    this.logger.debug(
      '[AppComponent] Asignando punto de emisión:',
      selectedPoint,
    );

    // Liberar el punto anterior antes de asignar el nuevo
    this.invoicePointSessionService
      .releaseInvoicePointWithHeartbeat()
      .catch(() => {})
      .then(() => {
    // Guardar datos en localStorage
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
        this.logger.debug(
          '[AppComponent] ✅ Punto de emisión asignado correctamente',
        );
        // Redirigir a welcome
        this.router.navigate(['/welcome']).then(() => {
          this.logger.debug(
            '[AppComponent] Redirigido a welcome después de asignar punto de emisión',
          );
        });
      })
      .catch((err) => {
        this.logger.error(
          '[AppComponent] ❌ Error al asignar punto de emisión:',
          err,
        );
        this.toast.httpError(
          err,
          'asignar punto de emisión',
          'Error al asignar punto de emisión. Intente nuevamente.',
        );
        // Redirigir a welcome aunque falle
        this.router.navigate(['/welcome']);
      });
    }); // release then assign
  }
  showConfigMenu(): boolean {
    // Usar UserRoleService para obtener roles de la compañía actual
    try {
      const roles = this.userRoleService.getCurrentRolesSync();
      if (roles && roles.length > 0) {
        return roles.includes('admin') || roles.includes('super_admin');
      }
    } catch (error) {
      // Si UserRoleService no está disponible, usar token como fallback
      this.logger.warn(
        '[AppComponent] No se pudo obtener roles de UserRoleService, usando token como fallback',
      );
    }

    // Fallback: usar token si UserRoleService no está disponible
    const token = localStorage.getItem(environment.TOKEN);
    if (!token) return false;
    const decoded = this.jwtHelper.decodeToken(token);
    const roles = decoded?.roles || [];
    if (typeof roles === 'string') {
      return roles === 'admin' || roles === 'super_admin';
    }
    if (Array.isArray(roles)) {
      return roles.includes('admin') || roles.includes('super_admin');
    }
    return false;
  }

  /**
   * Verifica si el usuario tiene múltiples empresas disponibles
   * @returns true si el usuario tiene más de una empresa
   */
  hasMultipleCompanies(): boolean {
    const token = decodeToken();
    if (!token) {
      this.logger.debug('[AppComponent] hasMultipleCompanies: No hay token');
      return false;
    }

    // El token puede tener companies (array de IDs) o companyEstablishments (array de objetos)
    let companies: any[] = [];

    if (token.companies && Array.isArray(token.companies)) {
      // Formato 1: token.companies es un array de IDs
      companies = token.companies;
    } else if (
      (token as any).companyEstablishments &&
      Array.isArray((token as any).companyEstablishments)
    ) {
      // Formato 2: token.companyEstablishments es un array de objetos con idCompany
      companies = (token as any).companyEstablishments;
    } else if (
      (token as any).companyIds &&
      Array.isArray((token as any).companyIds)
    ) {
      // Formato 3: token.companyIds es un array de IDs
      companies = (token as any).companyIds;
    }

    if (!Array.isArray(companies) || companies.length === 0) {
      this.logger.debug(
        '[AppComponent] hasMultipleCompanies: No se encontraron empresas en el token',
        {
          tokenKeys: Object.keys(token),
          hasCompanies: !!token.companies,
          hasCompanyEstablishments: !!(token as any).companyEstablishments,
          hasCompanyIds: !!(token as any).companyIds,
        },
      );
      return false;
    }

    // Si es un array de objetos (companyEstablishments), obtener los IDs únicos
    let uniqueCompanyIds: number[] = [];
    if (
      companies.length > 0 &&
      typeof companies[0] === 'object' &&
      companies[0].idCompany
    ) {
      // Es un array de objetos con idCompany
      uniqueCompanyIds = [...new Set(companies.map((c: any) => c.idCompany))];
    } else {
      // Es un array de IDs directamente
      uniqueCompanyIds = [...new Set(companies)];
    }

    const hasMultiple = uniqueCompanyIds.length > 1;
    this.logger.debug('[AppComponent] hasMultipleCompanies:', {
      companiesCount: uniqueCompanyIds.length,
      uniqueCompanyIds,
      hasMultiple,
      companiesFormat:
        companies.length > 0 && typeof companies[0] === 'object'
          ? 'objects'
          : 'ids',
    });

    return hasMultiple;
  }

  ngAfterViewInit(): void {
    // Auto-expandir panel activo después de que la vista esté inicializada
    this.scheduleExpandActivePanel(this.router.url, 300);
  }

  /**
   * Programa la expansión del panel activo con un delay
   * Cancela cualquier expansión pendiente antes de programar una nueva
   */
  private scheduleExpandActivePanel(url: string, delay: number): void {
    // Cancelar timeout anterior si existe
    if (this.expandPanelTimeout) {
      clearTimeout(this.expandPanelTimeout);
    }
    this.expandPanelTimeout = setTimeout(() => {
      this.expandActivePanel(url);
      this.expandPanelTimeout = null;
    }, delay);
  }

  // Auto-expandir el panel que contiene la ruta activa
  private expandActivePanel(currentUrl: string): void {
    if (!this.menus || this.menus.length === 0 || !this.expansionPanels) {
      return;
    }

    // Buscar qué menú contiene la ruta actual
    const activeMenuIndex = this.menus.findIndex((menu) => {
      // Verificar si la ruta actual coincide con algún sub-menu
      if (menu.subMenus && menu.subMenus.length > 0) {
        return menu.subMenus.some((sub) => {
          if (sub.url) {
            return currentUrl.startsWith(sub.url);
          }
          return false;
        });
      }
      // También verificar si la ruta coincide con el menú principal
      if (menu.url) {
        return currentUrl.startsWith(menu.url);
      }
      return false;
    });

    // Expandir el panel correspondiente
    if (activeMenuIndex >= 0 && this.expansionPanels.length > activeMenuIndex) {
      const panel = this.expansionPanels.toArray()[activeMenuIndex];
      if (panel && !panel.expanded) {
        panel.open();
        this.logger.debug(
          `Panel expandido automáticamente: ${this.menus[activeMenuIndex].name}`,
        );
      }
    }
  }

  /**
   * Determina si un grupo de menú debe estar expandido basándose en la ruta actual
   * @param menu Grupo de menú a verificar
   * @returns true si el grupo contiene la ruta activa
   */
  isMenuGroupExpanded(menu: Menu): boolean {
    if (!this.router) {
      return false;
    }
    const currentUrl = this.router.url;

    // Verificar si algún sub-menú coincide con la ruta actual
    if (menu.subMenus && menu.subMenus.length > 0) {
      return menu.subMenus.some((sub) => {
        if (sub.url) {
          return currentUrl.startsWith(sub.url);
        }
        return false;
      });
    }

    // Verificar si el menú principal coincide con la ruta actual
    if (menu.url) {
      return currentUrl.startsWith(menu.url);
    }

    return false;
  }

  /**
   * TrackBy function para optimizar el rendimiento del *ngFor
   */
  trackByMenuId(index: number, menu: Menu): number {
    return menu.id;
  }

  /**
   * TrackBy function para optimizar el rendimiento del *ngFor de sub-menús
   */
  trackBySubMenuId(index: number, subMenu: Menu): number {
    return subMenu.id;
  }

  /**
   * Verifica si una ruta está activa para mejorar accesibilidad
   */
  isRouteActive(url: string): boolean {
    if (!this.router || !url) {
      return false;
    }
    return this.router.url.startsWith(url);
  }

  /**
   * Lifecycle hook - Limpia todas las suscripciones para evitar memory leaks
   */
  ngOnDestroy(): void {
    // Cancelar timeout pendiente si existe
    if (this.expandPanelTimeout) {
      clearTimeout(this.expandPanelTimeout);
      this.expandPanelTimeout = null;
    }

    // Completar el Subject para limpiar todas las suscripciones
    this.destroy$.next();
    this.destroy$.complete();
  }
}
