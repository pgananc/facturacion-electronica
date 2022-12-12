import { MatPaginator } from '@angular/material/paginator';
import { Component, OnInit, ViewChild, Inject } from '@angular/core';
import { environment } from 'src/environments/environment';
import { Company } from '../../../_model/customer/company';
import { CompanyService } from '../../../_service/customer/company.service';
import { CompanyDialogComponent } from '../../welcome/company-dialog/company-dialog.component';
import { decodeToken } from '../../../_functions/functions';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSort } from '@angular/material/sort';

@Component({
  selector: 'app-company-info',
  templateUrl: './company-info.component.html',
  styleUrls: ['./company-info.component.css'],
})
export class CompanyInfoComponent implements OnInit {
  company: Company;
  displayedColumns = ['name', 'branchOfficeCode', 'principal', 'actions'];
  showCompanies: boolean = false;
  dataSource: MatTableDataSource<Company>;
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;
  constructor(
    private companyService: CompanyService,
    private dialogRef: MatDialogRef<CompanyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) private data: Company[]
  ) {}

  ngOnInit(): void {
    this.dataSource = new MatTableDataSource(this.data);
    this.dataSource.sort = this.sort;
    let idCompanySelected = Number(
      sessionStorage.getItem(environment.ID_COMPANY)
    );

    if (this.data.length > 1) {
      this.showCompanies = true;
      this.company = this.data.filter(
        (companySelected) => companySelected.idCompany === idCompanySelected
      )[0];
    } else {
      this.showCompanies = false;
      this.company = this.data[0];
    }
  }

  selectCompany(company: Company) {
    if (company != null && company.idCompany > 0) {
      sessionStorage.setItem(environment.ID_COMPANY, String(company.idCompany));
      sessionStorage.setItem(
        environment.IDENTIFICATION,
        company.identification
      );
      this.company = company;
      this.companyService.companyChange.next(company);
    } else {
      alert('Seleccione una compania');
    }
  }
  close() {
    this.dialogRef.close();
  }
}
