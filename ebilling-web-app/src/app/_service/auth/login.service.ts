import { environment } from './../../../environments/environment';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { User } from 'src/app/_model/auth/user';
import { TokenResponse } from '../../_model/auth/tokenResponse';

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  url: string = `${environment.HOST_AUTH}/api/login`;
  messageChange = new Subject<string>();
  constructor(private http: HttpClient, private router: Router) {}

  login(user: User) {
    return this.http.post<TokenResponse>(this.url, user);
  }
  closeSession() {
    sessionStorage.clear();
    this.router.navigate(['login']);
  }
  isLogin() {
    let token = sessionStorage.getItem(environment.TOKEN);
    return token != null;
  }

  sendMail(mail: string) {
    return this.http.post<number>(
      `${environment.HOST_AUTH}/api/mail/send`,
      mail,
      {
        headers: new HttpHeaders().set('Content-Type', 'text/plain'),
      }
    );
  }
}
