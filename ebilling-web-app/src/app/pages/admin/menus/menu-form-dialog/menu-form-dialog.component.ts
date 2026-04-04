import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
} from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { MenuAdminService } from '../../../../_service/admin/menu-admin.service';
import {
  MenuAdmin,
  CreateMenuRequest,
  UpdateMenuRequest,
} from '../../../../_model/admin/menu-admin';
import { ToastService } from '../../../../_service/notification/toast.service';
import { LoggerService } from '../../../../_service/logger/logger.service';
import { extractUserFriendlyErrorMessage } from '../../../../_utils/error-message.util';

export interface MenuFormDialogData {
  menu: MenuAdmin | null;
  parentMenu: MenuAdmin | null;
  isEdit: boolean;
}

/**
 * Diálogo para crear o editar un menú
 */
@Component({
  selector: 'app-menu-form-dialog',
  templateUrl: './menu-form-dialog.component.html',
  styleUrls: ['./menu-form-dialog.component.css'],
})
export class MenuFormDialogComponent implements OnInit, OnDestroy {
  form: FormGroup;
  isLoading = false;
  isEdit: boolean;
  parentMenu: MenuAdmin | null;
  featureCodeExists: boolean | null = null;
  checkingFeature = false;
  private destroy$ = new Subject<void>(); // Para limpiar suscripciones

  // Iconos Material comunes para sugerencias
  commonIcons = [
    'home',
    'dashboard',
    'receipt',
    'description',
    'people',
    'person',
    'store',
    'business',
    'settings',
    'admin_panel_settings',
    'security',
    'credit_card',
    'assessment',
    'bar_chart',
    'list',
    'add_circle',
    'edit',
    'delete',
    'search',
    'filter_list',
  ];

  constructor(
    public dialogRef: MatDialogRef<MenuFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MenuFormDialogData,
    private readonly fb: FormBuilder,
    private readonly menuService: MenuAdminService,
    private readonly toast: ToastService,
    private readonly logger: LoggerService
  ) {
    this.isEdit = data.isEdit;
    this.parentMenu = data.parentMenu;

    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      icon: ['', [Validators.required, Validators.maxLength(50)]],
      url: [null, [this.urlValidator]],
      featureCode: [null, [Validators.maxLength(50)]],
      active: [true],
      order: [null, [Validators.min(0)]],
    });
  }

  ngOnInit(): void {
    if (this.isEdit && this.data.menu) {
      this.populateForm(this.data.menu);
    }

    // Escuchar cambios en featureCode para validar si existe
    // Usar debounce para evitar llamadas HTTP con cada letra
    this.form
      .get('featureCode')
      ?.valueChanges.pipe(
        debounceTime(500), // Esperar 500ms después de que el usuario deje de escribir
        distinctUntilChanged(), // Solo ejecutar si el valor cambió realmente
        takeUntil(this.destroy$) // Limpiar suscripción al destruir el componente
      )
      .subscribe((value) => {
        if (value && value.trim().length >= 2) {
          // Solo validar si tiene al menos 2 caracteres (evita validar "W", "WE", etc.)
          this.checkFeatureExists(value.trim());
        } else {
          this.featureCodeExists = null;
          this.checkingFeature = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Validador personalizado para URL
   * Si es un menú padre (sin parent), la URL puede ser null
   * Si es un submenú, la URL debe ser válida
   */
  urlValidator = (control: AbstractControl): { [key: string]: any } | null => {
    const value = control.value;
    if (!value || (typeof value === 'string' && value.trim() === '')) {
      return null; // URL vacía es válida (menú contenedor)
    }
    // Validar formato básico de URL
    if (
      typeof value === 'string' &&
      (value.startsWith('/') ||
      value.startsWith('http://') ||
        value.startsWith('https://'))
    ) {
      return null;
    }
    return { invalidUrl: true };
  };

  /**
   * Pobla el formulario con los datos del menú a editar
   */
  private populateForm(menu: MenuAdmin): void {
    this.form.patchValue({
      name: menu.name,
      icon: menu.icon,
      url: menu.url,
      featureCode: menu.featureCode || null,
      active: menu.active,
      order: menu.order || null,
    });
  }

  /**
   * Verifica si un featureCode existe en feature_permission
   */
  private checkFeatureExists(featureCode: string): void {
    this.checkingFeature = true;
    this.menuService.checkFeatureExists(featureCode).subscribe({
      next: (response) => {
        this.featureCodeExists = response.exists;
        this.checkingFeature = false;
      },
      error: (error) => {
        // Si el endpoint no existe, asumir que no existe (para retrocompatibilidad)
        this.featureCodeExists = false;
        this.checkingFeature = false;
        this.logger.debug(
          'Endpoint check-feature no disponible, asumiendo que no existe'
        );
      },
    });
  }

  /**
   * Sincroniza el feature_permission después de crear/actualizar el menú
   */
  private syncFeaturePermission(
    featureCode: string,
    featureName: string
  ): Observable<void> {
    if (!featureCode || !featureCode.trim()) {
      return new Observable((observer) => {
        observer.next();
        observer.complete();
      });
    }

    return this.menuService.syncFeaturePermission(
      featureCode.trim(),
      featureName
    );
  }

  /**
   * Maneja el envío del formulario
   */
  onSubmit(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      this.toast.warning('Por favor completa todos los campos requeridos');
      return;
    }

    const formValue = this.form.value;
    const featureCode = formValue.featureCode?.trim() || null;
    const menuName = formValue.name;

    // Validar featureCode antes de enviar
    if (featureCode && this.featureCodeExists === false) {
      // Si el feature no existe, intentar crearlo primero
      this.toast.warning(
        `El feature "${featureCode}" no existe. Intentando crearlo automáticamente...`
      );
      this.syncFeaturePermission(featureCode, menuName).subscribe({
        next: () => {
          // Una vez creado, proceder con el guardado del menú
          this.saveMenu(formValue, featureCode);
        },
        error: (error) => {
          this.logger.error('❌ Error al crear feature:', error);
          
          // Si el endpoint no está implementado (405) o hay otro error, 
          // permitir guardar el menú sin el feature (el backend lo validará)
          if (error.status === 405) {
            // Endpoint sync-feature no implementado en el backend
            this.toast.warning(
              `El endpoint de creación automática no está disponible. El menú se guardará, pero el feature "${featureCode}" debe crearse manualmente en la base de datos o desde el backend.`
            );
            // Continuar guardando el menú - el backend validará si el feature existe
            this.saveMenu(formValue, featureCode);
          } else {
            // Otro error - mostrar mensaje y permitir intentar guardar de todas formas
            this.toast.error(
              `No se pudo crear el feature "${featureCode}" automáticamente. El menú se guardará, pero asegúrate de que el feature exista en la tabla feature_permission o el backend rechazará la operación.`
            );
            // Continuar guardando el menú - el backend validará si el feature existe
            this.saveMenu(formValue, featureCode);
          }
        },
      });
      return;
    }

    // Si el feature existe o no hay featureCode, guardar directamente
    this.saveMenu(formValue, featureCode);
  }

  /**
   * Guarda el menú (crear o actualizar)
   */
  private saveMenu(formValue: any, featureCode: string | null): void {
    this.isLoading = true;

    if (this.isEdit && this.data.menu) {
      // Actualizar menú existente
      // Actualización parcial: solo enviar campos que cambian
      const updateRequest: UpdateMenuRequest = {
        name: formValue.name,
        icon: formValue.icon,
        url: formValue.url?.trim() || null,
        active: formValue.active,
        order: formValue.order || null,
      };

      // Manejar featureCode según la documentación del backend:
      // - Si se quiere quitar: enviar string vacío ""
      // - Si cambió: enviar el nuevo valor
      // - Si no cambió: no enviar el campo (actualización parcial)
      const currentFeatureCode = this.data.menu.featureCode || null;
      const newFeatureCode = featureCode || null;

      if (currentFeatureCode !== newFeatureCode) {
        // Cambió el featureCode
        if (newFeatureCode === null || newFeatureCode === '') {
          // Quitar featureCode: enviar string vacío
          updateRequest.featureCode = '';
        } else {
          // Cambiar a nuevo featureCode
          updateRequest.featureCode = newFeatureCode;
        }
      }
      // Si no cambió, no enviar el campo (actualización parcial)

      // NOTA: parentId no se maneja en el formulario de edición actual
      // El parentId solo se establece al crear un submenú

      this.menuService.updateMenu(this.data.menu.id, updateRequest).subscribe({
        next: (updatedMenu) => {
          this.isLoading = false;
          this.dialogRef.close(true);
          if (updatedMenu.feature) {
            this.toast.success('Menú actualizado exitosamente');
          } else {
            this.toast.success('Menú actualizado exitosamente');
          }
        },
        error: (error) => {
          this.isLoading = false;
          this.logger.error('❌ Error al actualizar menú:', error);

          // Manejar error específico de feature code no encontrado
          if (
            error.status === 400 &&
            error.error?.message?.includes('Feature code')
          ) {
            this.toast.error(
              `El feature code "${featureCode}" no existe o está inactivo. Por favor créalo primero en la gestión de permisos.`
            );
          } else {
            // Usar mensaje amigable en lugar de mostrar error técnico
            const friendlyMessage = extractUserFriendlyErrorMessage(
              error,
              'actualizar el menú'
            );
            this.toast.error(friendlyMessage);
          }
        },
      });
    } else {
      // Crear nuevo menú
      const createRequest: CreateMenuRequest = {
        name: formValue.name,
        icon: formValue.icon,
        url: formValue.url?.trim() || null,
        featureCode: featureCode || null,
        parentId: this.parentMenu?.id || null,
        active: formValue.active,
        order: formValue.order || null,
      };

      this.menuService.createMenu(createRequest).subscribe({
        next: (createdMenu) => {
          this.isLoading = false;
          this.dialogRef.close(true);
          if (createdMenu.feature) {
            this.toast.success('Menú creado exitosamente');
          } else {
            this.toast.success('Menú creado exitosamente');
          }
        },
        error: (error) => {
          this.isLoading = false;
          this.logger.error('❌ Error al crear menú:', error);

          // Manejar error específico de feature code no encontrado
          if (
            error.status === 400 &&
            error.error?.message?.includes('Feature code')
          ) {
            this.toast.error(
              `El feature code "${featureCode}" no existe o está inactivo. Por favor créalo primero en la gestión de permisos.`
            );
          } else {
            // Usar mensaje amigable en lugar de mostrar error técnico SQL
            const friendlyMessage = extractUserFriendlyErrorMessage(
              error,
              'crear el menú'
            );
            this.toast.error(friendlyMessage);
          }
        },
      });
    }
  }

  /**
   * Cierra el diálogo sin guardar
   */
  onCancel(): void {
    this.dialogRef.close(false);
  }

  /**
   * Marca todos los campos del formulario como touched para mostrar errores
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  /**
   * Obtiene el mensaje de error para un campo
   */
  getErrorMessage(controlName: string): string {
    const control = this.form.get(controlName);
    if (control?.hasError('required')) {
      return 'Este campo es requerido';
    }
    if (control?.hasError('maxlength')) {
      return `Máximo ${control.errors?.['maxlength'].requiredLength} caracteres`;
    }
    if (control?.hasError('min')) {
      return `El valor mínimo es ${control.errors?.['min'].min}`;
    }
    if (control?.hasError('invalidUrl')) {
      return 'La URL debe comenzar con "/" o ser una URL completa';
    }
    return '';
  }
}
