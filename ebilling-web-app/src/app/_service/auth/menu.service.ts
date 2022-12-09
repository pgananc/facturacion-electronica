import { environment } from './../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Menu } from '../../_model/auth/menu';

@Injectable({
  providedIn: 'root',
})
export class MenuService {
  menuChange = new Subject<Menu[]>();

  url: string = `${environment.HOST_AUTH}/api/menu`;
  constructor(private http: HttpClient) {}

  findMenuByUser(userName: string) {
    return this.http.get<Menu[]>(`${this.url}/${userName}`);
  }
}
