import { environment } from './../../../environments/environment';
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Client } from '../../_model/customer/client';
import { HttpClient } from '@angular/common/http';
import { ClientResponse } from '../../_model/customer/client-response';
import { ClientRequestDto } from '../../_model/customer/client-request-dto';
import { ClientPageResponse } from '../../_model/customer/client-page-response';
import { ClientSaveDto } from '../../_model/customer/client-save-dto';

@Injectable({
  providedIn: 'root',
})
export class ClientService {
  clientsChange = new Subject<Client[]>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_CUSTOMERS}/api/client`;
  constructor(private readonly http: HttpClient) {}

  save(client: ClientSaveDto) {
    return this.http.post<Client>(this.url, client);
  }

  update(client: ClientSaveDto, id: number) {
    return this.http.patch<Client>(`${this.url}/${id}`, client);
  }

  findByCompany(companyIdentification: string) {
    return this.http.get<ClientResponse>(
      `${this.url}/company/${companyIdentification}`
    );
  }

  findById(id: number) {
    return this.http.get<ClientResponse>(`${this.url}/${id}`);
  }

  delete(idClient: number) {
    return this.http.delete(`${this.url}/${idClient}`);
  }

  findClientByIdentificationOrNameOrType(client: Client) {
    return this.http.post<ClientResponse>(`${this.url}/search`, client);
  }

  searchPageable(
    p: number,
    s: number,
    client: ClientRequestDto,
    sortField?: string,
    sortDirection?: string
  ) {
    let url = `${this.url}/pageable?page=${p}&size=${s}`;

    // Add sorting parameters ONLY if both field and direction are provided and valid
    if (
      sortField &&
      sortField.trim() !== '' &&
      sortDirection &&
      sortDirection.trim() !== ''
    ) {
      url += `&sort=${sortField},${sortDirection}`;
    }

    return this.http.post<ClientPageResponse>(url, client);
  }
}
