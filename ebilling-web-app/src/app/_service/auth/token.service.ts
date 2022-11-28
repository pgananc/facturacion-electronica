import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class TokenService {
  url: string = `${environment.HOST_AUTH}/api/token`;
  messageChange = new Subject<string>();
  constructor(private http: HttpClient, private router: Router) {}

  validateTokenReset(token: string) {
    return this.http.get<boolean>(
      `${this.url}/restore-password/validate/${token}`
    );
  }
}
