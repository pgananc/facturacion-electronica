import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Role } from '../../_model/auth/role';
import { HttpClient } from '@angular/common/http';
import { RoleResponse } from '../../_model/auth/roleResponse';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  roleChange = new Subject<Role[]>();
  messangeChange = new Subject<String>();
  url: string = `${environment.HOST_AUTH}/api/role`;

  constructor(private http: HttpClient) {}

  findAll() {
    return this.http.get<RoleResponse>(this.url);
  }
}
