import {BasicEntity} from '../basicEntity';
import {Company} from "./company";

export class CompanyResponse extends BasicEntity {
  companyDtos: Company[] = [];
  companyDto: Company;
}
