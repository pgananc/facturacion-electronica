import { BasicEntity } from '../basic-entity';
import { UserCompany } from './user-company';

export interface UserCompanyResponse extends BasicEntity {
  userCompanyDtos: UserCompany[];
  userCompanyDto: UserCompany;
}
