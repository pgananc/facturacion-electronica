import { Company } from './../../_model/customer/company';
import { Component, OnInit } from '@angular/core';
import { decodeToken } from 'src/app/_functions/functions';
import { CompanyService } from '../../_service/customer/company.service';
import { MatDialog } from '@angular/material/dialog';
import { CompanyDialogComponent } from './company-dialog/company-dialog.component';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-welcome',
  templateUrl: './welcome.component.html',
  styleUrls: ['./welcome.component.css'],
})
export class WelcomeComponent implements OnInit {
  company: Company;
  isWait: boolean = false;
  constructor(
    private companyService: CompanyService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    console.log('init welcome');
    let token = decodeToken();
    this.companyService.findByIds(token.companies).subscribe((data) => {
      this.isWait = true;
      if (data.code == 0 && data.companyDtos.length == 1) {
        sessionStorage.setItem(
          environment.ID_COMPANY,
          data.companyDtos[0]?.idCompany.toString()
        );
        sessionStorage.setItem(
          environment.IDENTIFICATION,
          data.companyDtos[0]?.identification
        );
        this.company = new Company();
        this.company = data.companyDtos[0];
        this.companyService.companyChange.next(this.company);
      } else if (data.code == 0 && data.companyDtos.length > 1) {
        this.openDialog(data.companyDtos);
      }
    });
  }

  openDialog(companies: Company[]) {
    const dialogRef = this.dialog.open(CompanyDialogComponent, {
      width: '800px',
      data: companies,
      disableClose: true,
    });
    dialogRef.afterClosed().subscribe((result) => {
      sessionStorage.setItem(environment.ID_COMPANY, result.idCompany);
      sessionStorage.setItem(environment.IDENTIFICATION, result.identification);
      this.companyService.companyChange.next(result);
      this.isWait = true;
    });
  }
}
