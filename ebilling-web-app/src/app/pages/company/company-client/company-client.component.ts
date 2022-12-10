import {Component, OnInit, ViewChild} from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatSort} from '@angular/material/sort';
import {MatTableDataSource} from '@angular/material/table';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute} from '@angular/router';
import {
  CLIENT_TYPE,
  DELETE,
  DURATION_TIME_MESSAGE,
  EMPTY_DATA,
  HEADER_MESSAGE,
  IDENTIFICATION_TYPE
} from '../../../_constants/constants';
import {FormGroup} from '@angular/forms';
import {Company} from "../../../_model/customer/company";
import {CompanyService} from "../../../_service/customer/company.service";

import {Client} from "../../../_model/customer/client";
import {CompanyClientService} from "../../../_service/customer/companyClient.service";

@Component({
  selector: 'app-company',
  templateUrl: 'company-client.component.html',
  styleUrls: ['company-client.component.css'],
})
export class CompanyClientComponent implements OnInit {
  form: FormGroup;
  companies: Company[] = [];
  companyId: number;
  clientId: number;
  displayedColumnsClient = ['identification', 'idType', 'name', 'clientType', 'actions'];
  dataSourceClient: MatTableDataSource<Client>;
  status: boolean = true;
  quantity: number = 0;

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private companyService: CompanyService,
    private companyClientService: CompanyClientService,
    private snackBar: MatSnackBar,
    public route: ActivatedRoute
  ) {
  }

  ngOnInit() {
    this.form = new FormGroup({});
    if (this.companies.length === 0) {
      this.companyService.findAll().subscribe(data => {
        this.companies = data.companyDtos;
      });
    }

    this.companyClientService.messageChange.subscribe((data) => {
      this.snackBar.open(data, HEADER_MESSAGE.MESSAGE_HEADER_INFO.message, {
        duration: 2000,
      });
    });

    /*
    let company = new Company();
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.branchOfficeCode = this.form.value['branchOfficeCode'];
    company.status = this.status;
    this.companyService.searchPageable(0, 10, company).subscribe((data) => {
      this.quantity = data.totalElements;
      this.dataSource = new MatTableDataSource(data.content);
      this.dataSource.sort = this.sort!;
    });*/
  }

  delete(companyId: number, clientId: number) {
    this.companyClientService.delete(companyId, clientId).subscribe(() => {
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

  search() {
    let company = new Company();
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.branchOfficeCode = this.form.value['branchOfficeCode'];
    company.status = this.status;
    this.companyClientService.searchPageable(0, 10, this.companyId).subscribe((data) => {
      if (data != null) {
        this.quantity = data.totalElements;
        this.dataSourceClient = new MatTableDataSource(data.content);
        for (let client of this.dataSourceClient.data) {
          client.idTypeEnum=IDENTIFICATION_TYPE.find(idType=>idType.code===client.idType)!.value;
          client.clientTypeEnum=CLIENT_TYPE.find(clientType=>clientType.code===client.clientType)!.value;
        }
        this.dataSourceClient.sort = this.sort!;
      } else {
        this.snackBar.open(EMPTY_DATA.MESSAGE_EMPTY_DATA.message, 'OK', {
          duration: DURATION_TIME_MESSAGE.value,
        });
      }
    });
  }

  showMore(e: any) {
    let company = new Company();
    company.identification = this.form.value['identification'];
    company.name = this.form.value['name'];
    company.branchOfficeCode = this.form.value['branchOfficeCode'];
    company.status = this.status;
    this.companyService
      .searchPageable(e.pageIndex, e.pageSize, this.companyId)
      .subscribe((data) => {
        this.quantity = data.totalElements;
        this.dataSourceClient = new MatTableDataSource(data.content);
        this.dataSourceClient.sort = this.sort!;
      });
  }
}
