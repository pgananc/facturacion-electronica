import { BasicEntity } from '../basic-entity';
import { Client } from './client';
import { CertificateInfo } from './certificate-info';
export interface CertificateInfoRespose extends BasicEntity {
  certificateInfoDto: CertificateInfo;
}
