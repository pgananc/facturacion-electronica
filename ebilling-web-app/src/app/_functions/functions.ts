import { JwtHelperService } from '@auth0/angular-jwt';
import { environment } from 'src/environments/environment';

export const decodeToken = () => {
  let token = localStorage.getItem(environment.TOKEN);
  if (!token) {
    return null;
  }
  const helper = new JwtHelperService();
  try {
    return helper.decodeToken(token);
  } catch (error) {
    console.error('Error decodificando token:', error);
    return null;
  }
};

/**
 * Verifica si el token es válido y no está expirado
 * @returns true si el token existe, es válido y no está expirado
 */
export const isTokenValid = (): boolean => {
  const token = localStorage.getItem(environment.TOKEN);
  if (!token) {
    return false;
  }

  const helper = new JwtHelperService();
  try {
    // Verificar si el token está expirado
    if (helper.isTokenExpired(token)) {
      return false;
    }

    // Verificar que se pueda decodificar
    const decoded = helper.decodeToken(token);
    return decoded !== null;
  } catch (error) {
    console.error('Error validando token:', error);
    return false;
  }
};

export const base64ToBlob = (
  base64: string,
  contentType: string = '',
): Blob => {
  const byteCharacters = atob(base64); // Decode Base64 string to a string of bytes
  const byteNumbers = new Array(byteCharacters.length);
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
  }
  const byteArray = new Uint8Array(byteNumbers); // Create a Uint8Array from byte numbers
  return new Blob([byteArray], { type: contentType }); // Create a Blob from the Uint8Array
};
