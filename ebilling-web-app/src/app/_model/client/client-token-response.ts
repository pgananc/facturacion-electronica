export interface ClientTokenResponse {
  status: boolean;
  creationDate: string;
  idToken: number;
  clientNumber: string;
  clientEmail: string;
  token: string;
  expiryDate: string;
  expired: boolean;
}
