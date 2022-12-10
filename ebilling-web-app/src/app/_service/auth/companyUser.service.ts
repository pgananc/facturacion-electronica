import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {CompanyResponse} from "../../_model/customer/companyResponse";
import {environment} from "../../../environments/environment";
import {UserCompanyRequest} from "../../_model/auth/userCompanyRequest";

@Injectable({
  providedIn: 'root',
})
export class CompanyUserService {
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_AUTH}/api/user-company`;

  constructor(private http: HttpClient) {
  }

  save(userCompanyRequest: UserCompanyRequest) {
    return this.http.post(this.url, userCompanyRequest);
  }

  delete(idCompany: number, idUser: number) {
    return this.http.delete(`${this.url}/idCompany/${idCompany}/idUser/${idUser}`);
  }

  findUsersByIdCompany(idCompany: number) {
    return this.http.get<CompanyResponse>(`${this.url}/idCompany/${idCompany}`);
  }

  findByIdCompanyAndIdUser(idCompany: number, idUser:number) {
    return this.http.get<CompanyResponse>(`${this.url}/idCompany/${idCompany}/idUser/${idUser}`);
  }

  searchPageable(p: number, s: number, idCompany: number) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      idCompany
    );
  }
}
