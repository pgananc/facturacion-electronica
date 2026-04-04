import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { CertificateInfoRespose } from 'src/app/_model/customer/certifcate-info-response';
import { CertificateInfo } from 'src/app/_model/customer/certificate-info';
import { DigitalSignature } from 'src/app/_model/customer/digital-signature';
import { DigitalSignatureResponse } from 'src/app/_model/customer/digital-signature-reponse';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root',
})
export class DigitalSignatureService {
  companiesChange = new Subject<DigitalSignature[]>();
  digitalSignatureChange = new Subject<DigitalSignature>();
  digitalSignatureNameChange = new Subject<string>();
  messageChange = new Subject<string>();
  url: string = `${environment.HOST_CUSTOMERS}/api/digital-signature`;
  constructor(private readonly http: HttpClient) {}
  save(signFile: File, idCompany: number, password: string) {
    const formData = new FormData();
    formData.append('signFile', signFile, signFile.name);
    formData.append('password', password);
    formData.append('idCompany', idCompany.toString());

    return this.http.post<DigitalSignatureResponse>(`${this.url}`, formData);
  }
  update(signFile: File, idCompany: number, password: string, id: number) {
    const formData = new FormData();
    formData.append('signFile', signFile, signFile.name);
    formData.append('password', password);
    formData.append('idCompany', idCompany.toString());
    formData.append('id', id.toString());
    return this.http.patch<DigitalSignatureResponse>(
      `${this.url}/${id}`,
      formData
    );
  }

  findByIdcompany(id: number) {
    return this.http.get<DigitalSignatureResponse>(`${this.url}/company/${id}`);
  }
  uploadFile(file: File, password: string) {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('password', password);
    return this.http.post<CertificateInfoRespose>(
      `${this.url}/upload`,
      formData
    );
  }
}
