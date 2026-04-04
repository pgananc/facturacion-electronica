import { MatPaginator } from '@angular/material/paginator';
import { Component, OnInit, ViewChild, Inject } from '@angular/core';
import { environment } from 'src/environments/environment';
import { CompanyFilter } from '../../../_model/customer/company-filter';
import { CompanyService } from '../../../_service/customer/company.service';
import { CompanyDialogComponent } from '../../welcome/company-dialog/company-dialog.component';
import { decodeToken } from '../../../_functions/functions';
import { MatTableDataSource } from '@angular/material/table';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSort } from '@angular/material/sort';
import { Router } from '@angular/router';
import { UserRoleService } from '../../../_service/auth/user-role.service';
import { LoggerService } from '../../../_service/logger/logger.service';

@Component({
  selector: 'app-company-info',
  templateUrl: './company-info.component.html',
  styleUrls: ['./company-info.component.css'],
})
export class CompanyInfoComponent implements OnInit {
  company: CompanyFilter;
  displayedColumns = ['name', 'branchOfficeCode', 'actions'];
  showCompanies: boolean = false;
  dataSource = new MatTableDataSource<CompanyFilter>([]);
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort: MatSort;
  constructor(
    private companyService: CompanyService,
    private dialogRef: MatDialogRef<CompanyDialogComponent>,
    private router: Router,
    private userRoleService: UserRoleService,
    private logger: LoggerService,
    @Inject(MAT_DIALOG_DATA) private data: CompanyFilter[]
  ) {}

  ngOnInit(): void {
    this.dataSource = new MatTableDataSource(this.data);
    this.dataSource.sort = this.sort;
    let idCompanySelected = Number(
      localStorage.getItem(environment.ID_COMPANY)
    );

    if (this.data.length > 1) {
      this.showCompanies = true;
      this.company = this.data.filter(
        (companySelected) =>
          companySelected.idCompany != null &&
          companySelected.idCompany === idCompanySelected
      )[0];
    } else {
      this.showCompanies = false;
      this.company = this.data[0];
    }
  }

  selectCompany(company: CompanyFilter) {
    if (company != null && company.idCompany != null && company.idCompany > 0) {
      localStorage.setItem(environment.ID_COMPANY, String(company.idCompany));
      localStorage.setItem(environment.IDENTIFICATION, company.identification);
      this.company = company;
      this.companyService.companyNameChange.next(company.name || '');

      // 🔄 Limpiar caché de roles de la compañía anterior
      this.userRoleService.clearCache();

      // 🔄 Cargar roles del usuario en la compañía seleccionada
      this.logger.debug(
        '[CompanyInfo] Cargando roles del usuario para la compañía seleccionada:',
        {
          companyId: company.idCompany,
          companyName: company.name,
        }
      );
      this.userRoleService.getRolesByCompany(company.idCompany).subscribe({
        next: (roles) => {
          this.logger.debug(
            `[CompanyInfo] Roles cargados para compañía ${company.idCompany}:`,
            roles
          );
        },
        error: (error) => {
          this.logger.error(
            '[CompanyInfo] Error al cargar roles del usuario:',
            error
          );
        },
      });
    }
  }
  close() {
    this.dialogRef.close(this.company);
    this.router.navigate(['/welcome']);
  }
}
