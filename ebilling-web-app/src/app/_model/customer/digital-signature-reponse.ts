import { DigitalSignature } from './digital-signature';
import { BasicEntity } from '../basic-entity';
export interface DigitalSignatureResponse extends BasicEntity {
  digitalSignatureDtos: DigitalSignature[];
  digitalSignatureDto: DigitalSignature;
}
