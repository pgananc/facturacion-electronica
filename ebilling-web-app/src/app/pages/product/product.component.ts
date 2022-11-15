import {Component, OnInit, ViewChild} from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {MatTableDataSource} from '@angular/material/table';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute} from '@angular/router';
import {DELETE, DURATION_TIME_MESSAGE, EMPTY_DATA, HEADER_MESSAGE, PRODUCT_TYPE,} from '../../_constants/constants';
import {FormControl, FormGroup} from '@angular/forms';
import {Product} from "../../_model/product/product";
import {ProductService} from "../../_service/product/product.service";

@Component({
  selector: 'app-product',
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css'],
})
export class ProductComponent implements OnInit {
  form: FormGroup;
  productTypes = PRODUCT_TYPE;
  displayedColumns = ['mainCode', 'auxiliarCode', 'name', 'description', 'unitPrice', 'discount', 'actions'];
  dataSource: MatTableDataSource<Product>;
  productType: number = 0;
  status: boolean = true;
  quantity: number = 0;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private productService: ProductService,
    private snackBar: MatSnackBar,
    public route: ActivatedRoute
  ) {
  }

  ngOnInit() {
    this.form = new FormGroup({
      mainCode: new FormControl(''),
      name: new FormControl(''),
    });
    this.status = true;
    this.productService.productChange.subscribe((data) => {
      this.dataSource = new MatTableDataSource(data);
      this.dataSource!.sort = this.sort!;
      this.dataSource!.paginator = this.paginator!;
    });

    this.productService.messageChange.subscribe((data) => {
      this.snackBar.open(data, HEADER_MESSAGE.MESSAGE_HEADER_INFO.message, {
        duration: 2000,
      });
    });
    let product = new Product();
    product.mainCode = this.form.value['mainCode'];
    product.name = this.form.value['name'];
    product.productType = this.productType;
    product.status = this.status;
    this.productService.searchPageable(0, 10, product).subscribe((data) => {
      this.quantity = data.totalElements;
      this.dataSource = new MatTableDataSource(data.content);
      this.dataSource.sort = this.sort!;
      //this.dataSource.paginator = this.paginator;
    });
  }

  delete(idProduct: number) {
    this.productService.delete(idProduct).subscribe(() => {
      this.productService.findAll().subscribe((data) => {
        if (data.code == 0) {
          this.productService.productChange.next(data.productDtos);
          this.productService.messageChange.next(
            DELETE.MESSAGE_DELETE_PRODUCT.message
          );
        } else {
          console.log(data.message);
        }
      });
    });
  }

  applyFilter(event: Event) {
    const input = (event.target as HTMLInputElement).value;
    this.dataSource!.filter = input.trim().toLowerCase();
  }

  search() {
    let product = new Product();
    product.mainCode = this.form.value['mainCode'];
    product.name = this.form.value['name'];
    product.productType = this.productType;
    product.status = this.status;
    this.productService.searchPageable(0, 10, product).subscribe((data) => {
      if (data != null) {
        this.quantity = data.totalElements;
        this.dataSource = new MatTableDataSource(data.content);
        this.dataSource.sort = this.sort!;
      } else {
        this.snackBar.open(EMPTY_DATA.MESSAGE_EMPTY_DATA.message, 'OK', {
          duration: DURATION_TIME_MESSAGE.value,
        });
      }
    });
  }

  limpiar() {
    this.form = new FormGroup({
      mainCode: new FormControl(''),
      name: new FormControl(''),
    });
    this.productType = 0;
    this.status = true;
  }

  showMore(e: any) {
    let product = new Product();
    product.mainCode = this.form.value['mainCode'];
    product.name = this.form.value['name'];
    product.productType = this.productType;
    product.status = this.status;
    this.productService
      .searchPageable(e.pageIndex, e.pageSize, product)
      .subscribe((data) => {
        this.quantity = data.totalElements;
        this.dataSource = new MatTableDataSource(data.content);
        this.dataSource.sort = this.sort!;
        //this.dataSource.paginator = this.paginator;
      });
  }
}
