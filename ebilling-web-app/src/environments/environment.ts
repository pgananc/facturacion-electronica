// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  HOST_AUTH: 'http://localhost:8280',
  HOST_CUSTOMERS: 'http://localhost:8180',
  HOST_PRODUCTS: 'http://localhost:8380',
  HOST_EBILLING: 'http://localhost:8480',
  HOST_REPORT: 'http://localhost:8580',
  USER: 'user',
  TOKEN: 'token',
  ID_COMPANY: 'idCompany',
  IDENTIFICATION: 'identification',
  NAME_COMPANY: 'nameCompany',
  ID_ESTABLISHMENT: 'idEstablishment',
  ID_INVOICE_POINT: 'idInvoicePoint',
  REINTENTOS: 3,
  // Timeout de inactividad en milisegundos (2 minutos = 120000 ms)
  INACTIVITY_TIMEOUT_MS: 2 * 60 * 1000, // 2 minutos
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
