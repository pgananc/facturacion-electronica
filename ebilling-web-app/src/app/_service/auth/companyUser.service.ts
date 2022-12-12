import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {CompanyResponse} from "../../_model/customer/companyResponse";
import {environment} from "../../../environments/environment";
import {UserCompanyRequest} from "../../_model/auth/userCompanyRequest";
import {UserCompanyResponse} from "../../_model/auth/userCompanyResponse";
import {UserCompany} from "../../_model/auth/userCompany";
import {Company} from "../../_model/customer/company";

@Injectable({
  providedIn: 'root',
})
export class CompanyUserService {
  userCompanyChange = new Subject<UserCompany[]>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_AUTH}/api/user-company`;

  constructor(private http: HttpClient) {
  }

  save(userCompanyRequest: UserCompanyRequest) {
    return this.http.post<{code:number,status:string}>(this.url, userCompanyRequest);
  }

  delete(idCompany: number, idUser: number) {
    return this.http.delete(`${this.url}/company/${idCompany}/user/${idUser}`);
  }

  findUsersByIdCompany(idCompany: number) {
    return this.http.get<UserCompanyResponse>(`${this.url}/company/${idCompany}`);
  }

  findByIdCompanyAndIdUser(idCompany: number, idUser:number) {
    return this.http.get<UserCompanyResponse>(`${this.url}/company/${idCompany}/user/${idUser}`);
  }

  searchPageable(p: number, s: number, idCompany: number) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      idCompany
    );
  }
}
