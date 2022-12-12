import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { switchMap } from 'rxjs';
import {
  DURATION_TIME_MESSAGE,
  EXIST_DATA,
  HEADER_MESSAGE,
  PRODUCT_TYPE,
  SUCCESS,
  UPDATE,
} from '../../../_constants/constants';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Product } from '../../../_model/product/product';
import { ProductService } from '../../../_service/product/product.service';

@Component({
  selector: 'app-update-product',
  templateUrl: './update-product.component.html',
  styleUrls: ['./update-product.component.css'],
})
export class UpdateProductComponent implements OnInit {
  idProduct: number = 0;
  product: Product = new Product();
  form: FormGroup;
  edition: boolean = false;
  productTypes = PRODUCT_TYPE;
  productType: number = 0;
  mainCodeLast: string = '';
  constructor(
    private productService: ProductService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.product = new Product();
    this.form = new FormGroup({
      id: new FormControl(0),
      mainCode: new FormControl('', Validators.required),
      auxiliarCode: new FormControl('', Validators.required),
      description: new FormControl(''),
      unitPrice: new FormControl(0),
      discount: new FormControl(0),
      name: new FormControl(''),
      status: new FormControl(true),
    });
    this.route.params.subscribe((params: Params) => {
      this.idProduct = params['idProduct'];
      this.edition = params['idProduct'] != null;
      this.initForm();
    });
  }
  initForm() {
    if (this.edition) {
      this.productService.findById(this.idProduct).subscribe((data) => {
        if (data.code == 0 && data.productDtos.length > 0) {
          let product = data.productDtos[0];
          this.productType = product.productType;
          this.mainCodeLast = product.mainCode;
          this.form = new FormGroup({
            id: new FormControl(product.idProduct),
            mainCode: new FormControl(product.mainCode),
            auxiliarCode: new FormControl(product.auxiliarCode),
            unitPrice: new FormControl(product.unitPrice),
            discount: new FormControl(product.discount),
            description: new FormControl(product.description),
            name: new FormControl(product.name),
            status: new FormControl(product.status),
          });
        }
      });
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
    this.productService
      .existsByMainCode(this.form.value['mainCode'])
      .subscribe((data) => {
        if (data) {
          this.snackBar.open(
            EXIST_DATA.MESSAGE_EXISTS_MAINCODE.message,
            HEADER_MESSAGE.MESSAGE_HEADER_INFO.message,
            {
              duration: DURATION_TIME_MESSAGE.value,
            }
          );
        } else {
          this.save();
        }
      });
  }

  save() {
    this.product.idProduct = this.idProduct;
    this.product.productType = this.productType;
    this.product.description = this.form.value['description'];
    this.product.mainCode = this.form.value['mainCode'];
    this.product.auxiliarCode = this.form.value['auxiliarCode'];
    this.product.name = this.form.value['name'];
    this.product.unitPrice = this.form.value['unitPrice'];
    this.product.discount = this.form.value['discount'];
    this.product.status = this.form.value['status'];

    if (this.product.idProduct > 0) {
      this.productService
        .update(this.product, this.product.idProduct)
        .pipe(
          switchMap(() => {
            return this.productService.findAll();
          })
        )
        .subscribe((data) => {
          this.productService.productChange.next(data.productDtos);
          this.productService.messageChange.next(
            UPDATE.MESSAGE_UPDATE_PRODUCT.message
          );
        });
    } else {
      this.productService
        .save(this.product)
        .pipe(
          switchMap(() => {
            return this.productService.findAll();
          })
        )
        .subscribe((data) => {
          this.productService.productChange.next(data.productDtos);
          this.productService.messageChange.next(
            SUCCESS.MESSAGE_REGISTER_PRODUCT.message
          );
        });
    }

    this.router.navigate(['product']);
  }

  get f() {
    return this.form.controls;
  }

  clearMainCode() {
    this.form.get('mainCode')?.reset();
  }
}
