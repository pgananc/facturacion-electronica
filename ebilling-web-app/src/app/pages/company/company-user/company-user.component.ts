import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import {
  DELETE,
  DURATION_TIME_MESSAGE,
  EMPTY_DATA,
  HEADER_MESSAGE,
} from '../../../_constants/constants';
import { FormGroup } from '@angular/forms';
import { Company } from '../../../_model/customer/company';
import { CompanyService } from '../../../_service/customer/company.service';
import { User } from '../../../_model/auth/user';
import { CompanyUserService } from '../../../_service/auth/companyUser.service';

@Component({
  selector: 'app-company',
  templateUrl: 'company-user.component.html',
  styleUrls: ['company-user.component.css'],
})
export class CompanyUserComponent implements OnInit {
  form: FormGroup;
  companies: Company[] = [];
  companyId: number;
  userId: number;
  displayedColumnsClient = ['name', 'username', 'mail', 'actions'];
  dataSourceUser: MatTableDataSource<User>;
  status: boolean = true;
  quantity: number = 0;

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private companyService: CompanyService,
    private companyUserService: CompanyUserService,
    private snackBar: MatSnackBar,
    public route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.form = new FormGroup({});
    if (this.companies.length === 0) {
      this.companyService.findAll().subscribe((data) => {
        this.companies = data.companyDtos;
      });
    }

    this.companyUserService.messageChange.subscribe((data) => {
      this.snackBar.open(data, HEADER_MESSAGE.MESSAGE_HEADER_INFO.message, {
        duration: 2000,
      });
    });
  }

  delete(companyId: number, userId: number) {
    this.companyUserService.delete(companyId, userId).subscribe(() => {
      this.companyService.findAll().subscribe((data) => {
        if (data.code == 0) {
          this.companyService.companiesChange.next(data.companyDtos);
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
    this.dataSourceUser!.filter = input.trim().toLowerCase();
  }

  search() {
    let user = new User();
    user.name = this.form.value['name'];
    user.userName = this.form.value['userName'];
    user.mail = this.form.value['mail'];
    this.companyUserService
      .searchPageable(0, 10, this.companyId)
      .subscribe((data) => {
        if (data != null) {
          this.quantity = data.totalElements;
          this.dataSourceUser = new MatTableDataSource(data.content);
          this.dataSourceUser.sort = this.sort!;
        } else {
          this.snackBar.open(EMPTY_DATA.MESSAGE_EMPTY_DATA.message, 'OK', {
            duration: DURATION_TIME_MESSAGE.value,
          });
        }
      });
  }

  showMore(e: any) {
    let user = new User();
    user.name = this.form.value['name'];
    user.userName = this.form.value['userName'];
    user.mail = this.form.value['mail'];
    this.companyUserService
      .searchPageable(e.pageIndex, e.pageSize, this.companyId)
      .subscribe((data) => {
        this.quantity = data.totalElements;
        this.dataSourceUser = new MatTableDataSource(data.content);
        this.dataSourceUser.sort = this.sort!;
      });
  }
}
