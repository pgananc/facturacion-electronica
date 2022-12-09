import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Company} from "../../_model/customer/company";
import {CompanyResponse} from "../../_model/customer/companyResponse";
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root',
})
export class CompanyClientService {
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_CUSTOMERS}/api/company-client`;

  constructor(private http: HttpClient) {
  }

  save(company: Company) {
    return this.http.post(this.url, company);
  }

  update(company: Company, id: number) {
    return this.http.patch(`${this.url}/${id}`, company);
  }

  findAll() {
    return this.http.get<CompanyResponse>(this.url);
  }

  findById(id: number) {
    return this.http.get<CompanyResponse>(`${this.url}/${id}`);
  }

  delete(idCompany: number, idClient: number) {
    return this.http.delete(`${this.url}/idCompany/${idCompany}/idClient/${idClient}`);
  }

  searchPageable(p: number, s: number, idCompany: number) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      idCompany
    );
  }
}
