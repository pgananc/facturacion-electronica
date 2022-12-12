import {BasicEntity} from '../basicEntity';
import {UserCompany} from "./userCompany";

export class UserCompanyResponse extends BasicEntity {
  userCompanyDtos: UserCompany[]=[];
  userCompanyDto: UserCompany;
}
