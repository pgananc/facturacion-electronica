import { CertificateInfo } from './certificate-info';

export interface DigitalSignature {
  id: number;
  signFile: string;
  password: string;
  effectiveDate: any;
  idCompany: number;
  certificateInfo?: CertificateInfo;
  fileName: string;
}
