import { Injectable } from '@angular/core';
import { User } from '../../_model/auth/user';
import { Subject } from 'rxjs';
import { environment } from 'src/environments/environment';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { UserResponse } from '../../_model/auth/userResponse';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  userChange = new Subject<User[]>();
  messangeChange = new Subject<string>();
  url: string = `${environment.HOST_AUTH}/api/user`;
  constructor(private http: HttpClient) {}

  save(user: User) {
    return this.http.post(this.url, user);
  }

  update(user: User, id: Number) {
    return this.http.patch(`${this.url}/${id}`, user);
  }

  searchPageable(p: number, s: number, user: User) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      user
    );
  }

  delete(idUser: number) {
    return this.http.delete(`${this.url}/${idUser}`);
  }

  findAll() {
    return this.http.get<UserResponse>(this.url);
  }

  findById(id: Number) {
    return this.http.get<UserResponse>(`${this.url}/${id}`);
  }

  existsByUserName(userName: String) {
    return this.http.get<boolean>(`${this.url}/exist/${userName}`);
  }

  restore(token: string, clave: string) {
    return this.http.post<boolean>(`${this.url}/restore/${token}`, clave, {
      headers: new HttpHeaders().set('Content-Type', 'text/plain'),
    });
  }
}
