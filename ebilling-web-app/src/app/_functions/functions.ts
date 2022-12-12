import { JwtHelperService } from '@auth0/angular-jwt';
import { environment } from 'src/environments/environment';

export const decodeToken = () => {
  let token = sessionStorage.getItem(environment.TOKEN)!;
  const helper = new JwtHelperService();
  return helper.decodeToken(token);
};
