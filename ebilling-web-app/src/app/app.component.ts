import { Component } from '@angular/core';
import { Menu } from './_model/auth/menu';
import { MenuService } from './_service/auth/menu.service';
import { LoginService } from './_service/auth/login.service';
import { CompanyService } from './_service/customer/company.service';
import { MatDialog } from '@angular/material/dialog';
import { CompanyInfoComponent } from './pages/company/company-info/company-info.component';
import { Company } from './_model/customer/company';
import { decodeToken } from './_functions/functions';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  title = 'ebilling-web-app';
  company = '';
  companies: Company[];

  menus: Menu[] = [];

  constructor(
    private menuService: MenuService,
    private companyService: CompanyService,
    public loginService: LoginService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    console.log('init app');
    this.company = '';
    this.menuService.menuChange.subscribe((data) => {
      this.menus = data;
    });
    this.companyService.companyChange.subscribe((data) => {
      this.company = data.name;
    });
  }
  openDialog(companies: Company[]) {
    const dialogRef = this.dialog.open(CompanyInfoComponent, {
      width: '800px',
      data: companies,
    });
  }

  showInfoCompany() {
    let token = decodeToken();
    if (token.companies != null && token.companies.length > 0) {
      this.companyService.findByIds(token.companies).subscribe((data) => {
        if (data.code === 0) {
          this.openDialog(data.companyDtos);
        }
      });
    }
  }
}
