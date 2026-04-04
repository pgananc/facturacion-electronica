import { BasicEntity } from '../basic-entity';
import { Company } from './company-establishment';

export interface CompanyResponse extends BasicEntity {
  companyDetailsDtos: Company[];
  companyDetailsDto: Company;
}
