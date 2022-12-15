import { environment } from './../../../environments/environment';
import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { LoginService } from '../../_service/auth/login.service';
import { MenuService } from '../../_service/auth/menu.service';
import { User } from '../../_model/auth/user';
import { JwtHelperService } from '@auth0/angular-jwt';
import { CompanyService } from '../../_service/customer/company.service';
import { Company } from 'src/app/_model/customer/company';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  userName: string = '';
  password: string = '';
  message: String = '';
  error: String = '';

  constructor(
    private loginService: LoginService,
    private menuService: MenuService,
    private companyService: CompanyService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}
  ngOnInit() {
    sessionStorage.clear();
    this.loginService.messageChange.subscribe((data) => {
      this.snackBar.open(data, 'Fail', {
        duration: 2000,
      });
    });
  }

  login() {
    let user = new User();
    user.userName = this.userName;
    user.password = this.password;
    this.loginService.login(user).subscribe((data) => {
      if (data.code == 0) {
        this.loginService.messageChange.next('Usuario / clave incorrecta');
      } else if (
        data.token != null &&
        data.token.token != null &&
        data.token.token != ''
      ) {
        sessionStorage.setItem(environment.TOKEN, data.token.token);
        const helper = new JwtHelperService();
        let decodedToken = helper.decodeToken(data.token.token);
        if (
          decodedToken.companies != null &&
          decodedToken.companies.length > 0
        ) {
          this.menuService
            .findMenuByUser(decodedToken.userName)
            .subscribe((data) => {
              this.menuService.menuChange.next(data);
              this.companyService.companyChange.next(new Company());
              this.router.navigate(['/welcome']);
            });
        } else {
          this.userName = '';
          this.password = '';
          sessionStorage.clear();
          this.loginService.messageChange.next(
            'Usuario no tiene asignado una compañia. Comuníquese con el administrador.'
          );
        }
      }
    });
  }
}
