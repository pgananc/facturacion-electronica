import {
  Component,
  Inject,
  OnInit,
  ViewChild,
  AfterViewInit,
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { FormControl } from '@angular/forms';
import { ProductDto } from '../../../_model/product/product-dto';
import { SelectionModel } from '@angular/cdk/collections';
import { LoggerService } from '../../../_service/logger/logger.service';

/**
 * SelectProductDialogComponent - Diálogo para seleccionar productos
 *
 * Permite buscar, filtrar y seleccionar múltiples productos para agregar a la factura.
 * Diseñado con el mismo estilo visual que el resto de la aplicación.
 */
@Component({
  selector: 'app-select-product-dialog',
  templateUrl: './select-product-dialog.component.html',
  styleUrls: ['./select-product-dialog.component.css'],
})
export class SelectProductDialogComponent implements OnInit, AfterViewInit {
  displayedColumns: string[] = [
    'select',
    'code',
    'name',
    'description',
    'unitPrice',
    'ivaTaxType',
    'iceTaxType',
  ];
  dataSource = new MatTableDataSource<ProductDto>([]);
  selection = new SelectionModel<ProductDto>(true, []);
  filterControl = new FormControl('');

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private readonly dialogRef: MatDialogRef<SelectProductDialogComponent>,
    @Inject(MAT_DIALOG_DATA)
    public data: { products: ProductDto[]; searchTerm: string },
    private readonly logger: LoggerService
  ) {
    // Asegurar que products es siempre un array válido
    const productsArray = Array.isArray(data?.products) ? data.products : [];
    this.dataSource = new MatTableDataSource(productsArray);
    this.logger.debug('Diálogo de productos inicializado', {
      totalProducts: productsArray.length,
      receivedData: data,
    });
  }

  ngOnInit(): void {
    // Configurar el filtro con el término de búsqueda inicial
    if (this.data.searchTerm) {
      this.filterControl.setValue(this.data.searchTerm);
      this.dataSource.filter = this.data.searchTerm.trim().toLowerCase();
    }

    // Suscribirse a cambios en el filtro
    this.filterControl.valueChanges.subscribe((value) => {
      this.applyFilter(value || '');
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;

    // ✅ Configurar ordenamiento por defecto: código ascendente
    // setTimeout para evitar ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      if (this.sort) {
        this.sort.active = 'code';
        this.sort.direction = 'asc';
        this.dataSource.sort = this.sort;
      }
    });

    // Configurar el filtro personalizado
    this.dataSource.filterPredicate = (data: ProductDto, filter: string) => {
      const searchStr = filter.toLowerCase();
      return (
        data.mainCode?.toLowerCase().includes(searchStr) ||
        data.name?.toLowerCase().includes(searchStr) ||
        data.description?.toLowerCase().includes(searchStr) ||
        false
      );
    };
  }

  /**
   * Verifica si todas las filas están seleccionadas
   */
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  /**
   * Alterna la selección de todas las filas
   */
  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.dataSource.data.forEach((row) => this.selection.select(row));
    }
  }

  /**
   * Cierra el diálogo sin seleccionar productos
   */
  onCancel(): void {
    this.dialogRef.close([]);
  }

  /**
   * Cierra el diálogo y retorna los productos seleccionados
   */
  onAdd(): void {
    if (this.selection.selected.length > 0) {
      this.logger.debug('Productos seleccionados para agregar', {
        count: this.selection.selected.length,
      });
      this.dialogRef.close(this.selection.selected);
    }
  }

  /**
   * Aplica el filtro a la tabla de productos
   */
  private applyFilter(filterValue: string): void {
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }
}
