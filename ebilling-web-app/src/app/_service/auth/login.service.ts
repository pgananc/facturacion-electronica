import { environment } from './../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { User } from 'src/app/_model/auth/user';

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  url: string = `${environment.HOST_AUTH}/login`;
  messageChange = new Subject<string>();
  constructor(private http: HttpClient, private router: Router) {}

  login(user: User) {
    return this.http.post(this.url, user);
  }
  closeSession() {
    sessionStorage.clear();
    this.router.navigate(['login']);
  }
  isLogin() {
    let user = sessionStorage.getItem(environment.USER);
    return user != null;
  }
}
