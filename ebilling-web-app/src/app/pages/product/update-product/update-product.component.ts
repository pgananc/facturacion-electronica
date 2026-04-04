import { Component, OnInit, Inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {
  filter,
  switchMap,
  catchError,
  of,
  map,
  startWith,
  Observable,
} from 'rxjs';
import {
  DURATION_TIME_MESSAGE,
  EXIST_DATA,
  HEADER_MESSAGE,
  PRODUCT_TYPE,
  SUCCESS,
  UPDATE,
} from '../../../_constants/constants';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ProductDto } from '../../../_model/product/product-dto';
import { TaxTypeDto } from '../../../_model/product/tax-type-dto';
import { ProductService } from '../../../_service/product/product.service';
import { TaxTypeService } from '../../../_service/product/tax-type.service';
import { environment } from 'src/environments/environment';
import { LoggerService } from '../../../_service/logger/logger.service';
import { ToastService } from '../../../_service/notification/toast.service';

export interface ProductDialogData {
  product?: ProductDto;
  isEdit: boolean;
}

@Component({
  selector: 'app-update-product',
  templateUrl: './update-product.component.html',
  styleUrls: ['./update-product.component.css'],
})
export class UpdateProductComponent implements OnInit {
  idProduct: number = 0;
  product: ProductDto;
  form: FormGroup;
  edition: boolean = false;
  productTypes = PRODUCT_TYPE;
  productType: number = 0;
  mainCodeLast: string = '';
  idCompany = 0;
  ivaTaxTypes: TaxTypeDto[] = []; // Lista de IVAs (siempre cargados)
  iceTaxTypes: TaxTypeDto[] = []; // Lista de ICEs (cargados si ice=true)

  // ✅ Autocomplete: Control y observable para filtrar ICEs
  iceFilterControl = new FormControl<string | TaxTypeDto | null>(null);
  filteredIceTaxTypes$: Observable<TaxTypeDto[]>;

  constructor(
    private readonly productService: ProductService,
    private readonly taxTypeService: TaxTypeService,
    private readonly snackBar: MatSnackBar,
    private readonly logger: LoggerService,
    private readonly toast: ToastService,
    public dialogRef: MatDialogRef<UpdateProductComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ProductDialogData
  ) {}

  ngOnInit(): void {
    this.product = this.createEmptyProduct();
    this.edition = this.data.isEdit;

    this.form = new FormGroup({
      id: new FormControl(0),
      mainCode: new FormControl('', Validators.required),
      name: new FormControl('', Validators.required),
      unitPrice: new FormControl(''),
      description: new FormControl(''),
      ivaTaxType: new FormControl(null, Validators.required), // ✅ IVA obligatorio
      iceTaxType: new FormControl(null), // ✅ ICE opcional
      tourismIva: new FormControl(false),
      ice: new FormControl(false),
      status: new FormControl(true),
    });

    // ✅ Cargar IVAs al iniciar (siempre necesarios)
    this.loadIvaTaxTypes();

    // ✅ Suscribirse al cambio del checkbox 'ice'
    this.form.get('ice')?.valueChanges.subscribe((hasIce: boolean) => {
      const iceTaxTypeControl = this.form.get('iceTaxType');

      if (hasIce) {
        // Si marca ICE, el combo se vuelve OBLIGATORIO
        iceTaxTypeControl?.setValidators([Validators.required]);
        this.loadIceTaxTypes(); // Cargar ICEs
      } else {
        // Si desmarca ICE, el combo deja de ser obligatorio
        iceTaxTypeControl?.clearValidators();
        this.iceTaxTypes = []; // Limpiar ICEs
        this.form.patchValue({ iceTaxType: null }); // Limpiar selección
        this.iceFilterControl.setValue(''); // Limpiar autocomplete
      }

      iceTaxTypeControl?.updateValueAndValidity(); // Actualizar estado de validación
    });

    if (this.edition && this.data.product) {
      this.idProduct = this.data.product.idProduct!;
      this.initForm();
    }
  }

  /**
   * Carga solo los tipos de IVA (Estrategia B: Query específico)
   */
  loadIvaTaxTypes(): void {
    this.taxTypeService.getTaxTypes(false, 'IVA').subscribe({
      next: (taxTypes) => {
        this.ivaTaxTypes = taxTypes;

        if (taxTypes.length === 0) {
          this.toast.warning(
            'No hay tipos de IVA disponibles. Por favor, configure los tipos de IVA en el sistema.'
          );
        }
      },
      error: (error) => {
        this.logger.error('Error al cargar tipos de IVA:', error);
        this.toast.error('Error al cargar tipos de IVA.');
      },
    });
  }

  /**
   * Carga solo los tipos de ICE (Estrategia B: Query específico)
   * Solo se llama si el checkbox 'ice' está marcado
   */
  loadIceTaxTypes(): void {
    this.taxTypeService.getTaxTypes(false, 'ICE').subscribe({
      next: (taxTypes) => {
        this.iceTaxTypes = taxTypes;

        // ✅ Configurar el filtrado reactivo del autocomplete
        this.setupIceAutocomplete();

        if (taxTypes.length === 0) {
          this.toast.warning(
            'No hay tipos de ICE disponibles. Por favor, configure los tipos de ICE en el sistema.'
          );
        }
      },
      error: (error) => {
        this.logger.error('Error al cargar tipos de ICE:', error);
        this.toast.error('Error al cargar tipos de ICE.');
      },
    });
  }

  /**
   * Configura el filtrado automático del autocomplete ICE
   */
  private setupIceAutocomplete(): void {
    this.filteredIceTaxTypes$ = this.iceFilterControl.valueChanges.pipe(
      startWith('' as string | TaxTypeDto | null),
      map((value: string | TaxTypeDto | null) => {
        // Si es un objeto TaxTypeDto, extraer el nombre para filtrado
        if (!value) return this.iceTaxTypes;

        const filterValue =
          typeof value === 'string' ? value : (value as TaxTypeDto).name || '';
        return this.filterIceTaxTypes(filterValue);
      })
    );

    // Suscribirse a la selección para actualizar el formulario
    this.iceFilterControl.valueChanges.subscribe(
      (value: string | TaxTypeDto | null) => {
        if (
          value &&
          typeof value === 'object' &&
          (value as TaxTypeDto).idTaxType
        ) {
          this.form.patchValue({ iceTaxType: value }, { emitEvent: false });
        }
      }
    );
  }

  /**
   * Filtra los tipos de ICE según el texto ingresado
   */
  private filterIceTaxTypes(value: string): TaxTypeDto[] {
    if (!value) {
      return this.iceTaxTypes;
    }

    const filterValue = value.toLowerCase();
    return this.iceTaxTypes.filter((ice) =>
      ice.name.toLowerCase().includes(filterValue)
    );
  }

  /**
   * Muestra el nombre del ICE en el autocomplete
   */
  displayIceTaxType(ice: TaxTypeDto | null): string {
    return ice ? ice.name : '';
  }

  /**
   * Cuando se selecciona una opción del autocomplete ICE,
   * sincronizar con el control del formulario
   */
  onIceOptionSelected(event: any): void {
    const selectedIce = event.option.value as TaxTypeDto;
    this.form.get('iceTaxType')?.setValue(selectedIce);
    this.logger.info('ICE seleccionado:', selectedIce.name);
  }

  /**
   * Limpiar selección de ICE
   */
  clearIceSelection(event: Event): void {
    event.stopPropagation();
    this.iceFilterControl.setValue(null);
    this.form.get('iceTaxType')?.setValue(null);
    this.logger.info('Selección de ICE limpiada');
  }

  initForm() {
    if (this.edition && this.data.product) {
      const product = this.data.product;
      this.product = { ...product }; // Clonar para evitar mutaciones
      this.productType = product.productType!;
      this.mainCodeLast = product.mainCode!;

      // Si tiene ICE, cargar los tipos de ICE
      if (product.ice) {
        this.loadIceTaxTypes();
      }

      this.form.patchValue({
        id: product.idProduct,
        mainCode: product.mainCode,
        name: product.name,
        unitPrice: product.unitPrice,
        description: product.description,
        ivaTaxType: product.ivaTaxType?.idTaxType || null, // ✅ IVA
        iceTaxType: product.iceTaxType || null, // ✅ ICE (objeto completo)
        tourismIva: product.tourismIva,
        ice: product.ice,
        status: product.status,
      });

      // ✅ Sincronizar iceFilterControl con el valor existente de ICE
      if (product.ice && product.iceTaxType) {
        this.iceFilterControl.setValue(product.iceTaxType);
      }
    }
  }

  validateSave() {
    if (this.edition && this.mainCodeLast === this.form.value['mainCode']) {
      this.save();
    } else {
      this.validateMainCode();
    }
  }

  validateMainCode() {
    const mainCode = this.form.value['mainCode'];
    // Obtener el ID de la compañía del usuario logueado
    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY)!);
    
    if (!idCompany) {
      this.toast.error('Error: No se encontró la compañía del usuario');
      return;
    }

    const exists$ = this.productService.existsByMainCode(mainCode, idCompany);

    // 1) Solo los "true" entran aquí → muestro el toast
    exists$.pipe(filter((exists) => exists)).subscribe(() => {
      this.toast.warning(EXIST_DATA.MESSAGE_EXISTS_MAINCODE.message);
    });

    // 2) Solo los "false" entran aquí → llamo a save()
    exists$.pipe(filter((exists) => !exists)).subscribe(() => this.save());
  }

  save() {
    const idCompany = Number(localStorage.getItem(environment.ID_COMPANY)!);

    if (!idCompany) {
      this.logger.error('No se encontró ID de compañía en localStorage');
      this.toast.error('Error: No se encontró información de la compañía');
      return;
    }

    // Construir el producto a enviar
    this.product.idCompany = idCompany;
    this.product.mainCode = this.form.value['mainCode'];
    this.product.name = this.form.value['name'];
    this.product.productType = this.productType;
    this.product.unitPrice = this.form.value['unitPrice'];
    this.product.description = this.form.value['description'];
    this.product.tourismIva = this.form.value['tourismIva'];
    this.product.ice = this.form.value['ice'];
    this.product.status = this.form.value['status'];

    // ✅ Solo enviar idProduct si es edición (mayor a 0)
    if (this.idProduct > 0) {
      this.product.idProduct = this.idProduct;
    } else {
      this.product.idProduct = undefined; // No enviar idProduct para productos nuevos
    }

    // ✅ Asignar ivaTaxType (obligatorio)
    const selectedIvaId = this.form.value['ivaTaxType'];
    if (selectedIvaId) {
      const selectedIva = this.ivaTaxTypes.find(
        (tt) => tt.idTaxType === selectedIvaId
      );
      if (selectedIva) {
        this.product.ivaTaxType = {
          idTaxType: selectedIva.idTaxType,
          type: selectedIva.type,
          name: selectedIva.name,
          value: selectedIva.value,
        };
      } else {
        this.logger.error('IVA seleccionado no encontrado en la lista');
        return; // No continuar si no hay IVA
      }
    } else {
      this.logger.error('IVA es obligatorio');
      this.toast.warning('Debe seleccionar un tipo de IVA');
      return;
    }

    // ✅ Asignar iceTaxType (OBLIGATORIO si ice=true)
    if (this.product.ice) {
      const selectedIce = this.form.value['iceTaxType'] as TaxTypeDto;
      if (selectedIce && selectedIce.idTaxType) {
        // El autocomplete devuelve el objeto completo
        this.product.iceTaxType = {
          idTaxType: selectedIce.idTaxType,
          type: selectedIce.type,
          name: selectedIce.name,
          value: selectedIce.value,
        };
      } else {
        // ❌ Si ice=true pero no hay ICE seleccionado, mostrar error
        this.logger.error(
          'ICE es obligatorio cuando el checkbox ICE está marcado'
        );
        this.toast.warning(
          'Debe seleccionar un tipo de ICE cuando marca el checkbox ICE'
        );
        return; // No continuar con el guardado
      }
    } else {
      this.product.iceTaxType = null; // Si ice=false, no enviar ICE
    }

    this.logger.debug('[ProductComponent] Producto a guardar:', this.product);

    if (this.product.idProduct && this.product.idProduct > 0) {
      this.productService
        .update(this.product, this.product.idProduct)
        .pipe(
          catchError((error) => {
            this.logger.error('Error al actualizar el producto', error);
            this.toast.error('Error al actualizar el producto');
            return of(null);
          }),
          switchMap((result) => {
            if (result) {
              return this.productService.findByIdCompany(idCompany).pipe(
                catchError((error) => {
                  this.logger.warn(
                    'Error al recargar lista de productos',
                    error
                  );
                  return of(null);
                })
              );
            }
            return of(null);
          })
        )
        .subscribe((data) => {
          if (data) {
            this.productService.productChange.next(data.productDtos);
            // ✅ Mostrar toast personalizado de éxito
            this.toast.success(UPDATE.MESSAGE_UPDATE_PRODUCT.message);
          }
          this.dialogRef.close(true); // Cerrar modal con éxito
        });
    } else {
      // Crear nuevo producto
      this.productService
        .save(this.product)
        .pipe(
          catchError((error) => {
            this.logger.error('Error al guardar el producto', error);
            this.toast.error('Error al guardar el producto');
            return of(null);
          }),
          switchMap((result) => {
            if (result) {
              return this.productService.findByIdCompany(idCompany).pipe(
                catchError((error) => {
                  this.logger.warn(
                    'Error al recargar lista de productos',
                    error
                  );
                  return of(null);
                })
              );
            }
            return of(null);
          })
        )
        .subscribe((data) => {
          if (data) {
            this.productService.productChange.next(data.productDtos);
            // ✅ Mostrar toast personalizado de éxito
            this.toast.success(SUCCESS.MESSAGE_REGISTER_PRODUCT.message);
            // Retornar el código principal del producto creado para poder buscarlo después
            this.dialogRef.close({ mainCode: this.product.mainCode });
          } else {
            this.dialogRef.close(false);
          }
        });
    }
  }

  /**
   * Cierra el modal sin guardar
   */
  cancel(): void {
    this.dialogRef.close(false);
  }

  get f() {
    return this.form.controls;
  }

  clearMainCode() {
    this.form.get('mainCode')?.reset();
  }

  private createEmptyProduct(): ProductDto {
    return {
      idProduct: 0,
      mainCode: '',
      description: '',
      unitPrice: 0,
      name: '',
      tourismIva: false,
      ice: false,
      status: true,
      ivaTaxType: {
        idTaxType: 0,
        type: 'IVA',
        name: '',
        value: 0,
      },
      iceTaxType: null,
    };
  }
}
