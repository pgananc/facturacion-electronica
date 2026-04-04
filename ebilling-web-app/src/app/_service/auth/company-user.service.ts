import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { CompanyResponse } from '../../_model/customer/company-response';
import { environment } from '../../../environments/environment';
import { UserCompanyRequest } from '../../_model/auth/user-company-request';
import { UserCompanyResponse } from '../../_model/auth/user-company-response';
import { UserResponse } from '../../_model/auth/user-response';
import { UserCompany } from '../../_model/auth/user-company';
import { CompanyFilter } from '../../_model/customer/company-filter';

@Injectable({
  providedIn: 'root',
})
export class CompanyUserService {
  userCompanyChange = new Subject<UserCompany[]>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_AUTH}/api/user-company`;

  constructor(private http: HttpClient) {}

  save(userCompanyRequest: UserCompanyRequest) {
    return this.http.post<{ code: number; status: string }>(
      this.url,
      userCompanyRequest
    );
  }

  delete(idCompany: number, idUser: number) {
    return this.http.delete(`${this.url}/company/${idCompany}/user/${idUser}`);
  }

  findUsersByIdCompany(idCompany: number) {
    return this.http.get<UserCompanyResponse>(
      `${this.url}/company/${idCompany}`
    );
  }

  findByIdCompanyAndIdUser(idCompany: number, idUser: number) {
    // Endpoint correcto: GET /api/user-company/user/{idUser}/company/{idCompany}
    // Retorna UserResponse con userDtos que contiene el usuario y sus roles en el contexto de la compañía
    const url = `${this.url}/user/${idUser}/company/${idCompany}`;


    return this.http.get<UserResponse>(url);
  }

  searchPageable(p: number, s: number, idCompany: number) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      idCompany
    );
  }
}
