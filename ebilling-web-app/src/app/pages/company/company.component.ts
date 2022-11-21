import {Component, OnInit, ViewChild} from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {MatTableDataSource} from '@angular/material/table';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute} from '@angular/router';
import {DELETE, DURATION_TIME_MESSAGE, EMPTY_DATA, HEADER_MESSAGE,} from '../../_constants/constants';
import {FormControl, FormGroup} from '@angular/forms';
import {Company} from "../../_model/customer/company";
import {CompanyService} from "../../_service/customer/company.service";

@Component({
  selector: 'app-company',
  templateUrl: './company.component.html',
  styleUrls: ['./company.component.css'],
})
export class CompanyComponent implements OnInit {
  form: FormGroup;
  displayedColumns = ['identification', 'branchOfficeCode','forceToAccounting','specialTaxpayer','principal', 'name', 'actions'];
  dataSource: MatTableDataSource<Company>;
  status: boolean = true;
  quantity: number = 0;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private companyService: CompanyService,
    private snackBar: MatSnackBar,
    public route: ActivatedRoute
  ) {
  }

  ngOnInit() {
    this.form = new FormGroup({
      identification: new FormControl(''),
      branchOfficeCode: new FormControl(''),
      name: new FormControl(''),
    });
    this.status = true;
    this.companyService.companyChange.subscribe((data) => {
      this.dataSource = new MatTableDataSource(data);
      this.dataSource!.sort = this.sort!;
      this.dataSource!.paginator = this.paginator!;
    });

    this.companyService.messageChange.subscribe((data) => {
      this.snackBar.open(data, HEADER_MESSAGE.MESSAGE_HEADER_INFO.message, {
        duration: 2000,
      });
    });
    let company = new Company();
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.branchOfficeCode = this.form.value['branchOfficeCode'];
    company.status = this.status;
    this.companyService.searchPageable(0, 10, company).subscribe((data) => {
      this.quantity = data.totalElements;
      this.dataSource = new MatTableDataSource(data.content);
      this.dataSource.sort = this.sort!;
      //this.dataSource.paginator = this.paginator;
    });
  }

  delete(idProduct: number) {
    this.companyService.delete(idProduct).subscribe(() => {
      this.companyService.findAll().subscribe((data) => {
        if (data.code == 0) {
          this.companyService.companyChange.next(data.companyDtos);
          this.companyService.messageChange.next(
            DELETE.MESSAGE_DELETE_COMPANY.message
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
    let company = new Company();
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.branchOfficeCode = this.form.value['branchOfficeCode'];
    company.status = this.status;
    this.companyService.searchPageable(0, 10, company).subscribe((data) => {
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
      identification: new FormControl(''),
      branchOfficeCode: new FormControl(''),
      name: new FormControl(''),
    });
    this.status = true;
  }

  showMore(e: any) {
    let company = new Company();
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.branchOfficeCode = this.form.value['branchOfficeCode'];
    company.status = this.status;
    this.companyService
      .searchPageable(e.pageIndex, e.pageSize, company)
      .subscribe((data) => {
        this.quantity = data.totalElements;
        this.dataSource = new MatTableDataSource(data.content);
        this.dataSource.sort = this.sort!;
        //this.dataSource.paginator = this.paginator;
      });
  }
}
