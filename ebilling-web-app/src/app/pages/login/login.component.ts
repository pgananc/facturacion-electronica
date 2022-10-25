import { environment } from './../../../environments/environment';
import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { LoginService } from '../../_service/auth/login.service';
import { MenuService } from '../../_service/auth/menu.service';
import { User } from '../../_model/auth/user';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  userName: string = '';
  password: String = '';
  message: String = '';
  error: String = '';

  constructor(
    private loginService: LoginService,
    private menuService: MenuService,
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
      if (data == 0) {
        this.loginService.messageChange.next('User / password incorrect');
      } else {
        sessionStorage.setItem(environment.USER, this.userName);
        this.menuService.findMenuByUser(this.userName).subscribe((data) => {
          this.menuService.menuChange.next(data);
          this.router.navigate(['/welcome']);
        });
      }
    });
  }
}
