import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {CompanyFilter} from "../../_model/customer/company-filter";
import {CompanyResponse} from "../../_model/customer/company-response";
import {environment} from "../../../environments/environment";

@Injectable({
  providedIn: 'root',
})
export class CompanyClientService {
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_CUSTOMERS}/api/company-client`;

  constructor(private http: HttpClient) {
  }

  save(company: CompanyFilter) {
    return this.http.post(this.url, company);
  }

  update(company: CompanyFilter, id: number) {
    return this.http.patch(`${this.url}/${id}`, company);
  }

  findAll() {
    return this.http.get<CompanyResponse>(this.url);
  }

  findById(id: number) {
    return this.http.get<CompanyResponse>(`${this.url}/${id}`);
  }

  delete(idCompany: number, idClient: number) {
    return this.http.delete(`${this.url}/company/${idCompany}/client/${idClient}`);
  }

  searchPageable(p: number, s: number, idCompany: number) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      idCompany
    );
  }
}
