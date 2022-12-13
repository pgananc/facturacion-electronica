import { environment } from './../../../environments/environment';
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Client } from '../../_model/customer/client';
import { HttpClient } from '@angular/common/http';
import { ClientResponse } from '../../_model/customer/clientResponse';

@Injectable({
  providedIn: 'root',
})
export class ClientService {
  clientChange = new Subject<Client[]>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_CUSTOMERS}/api/client`;
  constructor(private http: HttpClient) {}

  save(client: Client) {
    return this.http.post(this.url, client);
  }

  update(client: Client, id: Number) {
    return this.http.patch(`${this.url}/${id}`, client);
  }

  findByCompany(companyIdentification: string) {
    return this.http.get<ClientResponse>(
      `${this.url}/company/${companyIdentification}`
    );
  }

  findById(id: Number) {
    return this.http.get<ClientResponse>(`${this.url}/${id}`);
  }

  delete(idClient: number) {
    return this.http.delete(`${this.url}/${idClient}`);
  }

  findClientByIdentificationOrNameOrType(client: Client) {
    return this.http.post<ClientResponse>(`${this.url}/search`, client);
  }

  searchPageable(p: number, s: number, client: Client) {
    return this.http.post<any>(
      `${this.url}/pageable?page=${p}&size=${s}`,
      client
    );
  }

  existsByCompanyIdentificationAndClientIdentification(
    companyIdentification: string,
    identification: string
  ) {
    return this.http.get<boolean>(
      `${this.url}/exist/company/${companyIdentification}/client/${identification}`
    );
  }
}
