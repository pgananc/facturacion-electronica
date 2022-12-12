import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Company } from '../../../_model/customer/company';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';

@Component({
  selector: 'app-company-dialog',
  templateUrl: './company-dialog.component.html',
  styleUrls: ['./company-dialog.component.css'],
})
export class CompanyDialogComponent implements OnInit {
  displayedColumns = ['name', 'branchOfficeCode', 'principal', 'actions'];
  dataSource: MatTableDataSource<Company>;

  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;

  constructor(
    private dialogRef: MatDialogRef<CompanyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) private data: Company[]
  ) {}

  ngOnInit(): void {
    this.dataSource = new MatTableDataSource(this.data);
    this.dataSource.sort = this.sort;
  }

  selectCompany(company: Company) {
    if (company != null && company.idCompany > 0) {
      this.dialogRef.close(company);
    } else {
      alert('Seleccione una compania');
    }
  }
}
