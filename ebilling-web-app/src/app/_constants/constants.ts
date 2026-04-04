//Message general code > -1000
export const ERRORS = {
  MESSAGE_ERROR_REGISTER: {
    code: -1000,
    message: 'Error al procesar registro.',
  },
  MESSAGE_ERROR_NOT_AVAILABLE: {
    code: -1001,
    message: 'Servicio no disponible intente más tarde.',
  },
  MESSAGE_ERROR: {
    code: -1002,
    message: 'Ha ocurrido un error intente más tarde.',
  },
  MESSAGE_ERROR_NOT_FOUND: {
    code: -1003,
    message: 'Página no se ha cargado correctamente.',
  },
};

//Message success code > 2000
export const SUCCESS = {
  MESSAGE_REGISTER_CLIENT: {
    code: 2000,
    message: 'Cliente guardado correctamente.',
  },
  MESSAGE_REGISTER_PRODUCT: {
    code: 2001,
    message: 'Producto guardado correctamente.',
  },
  MESSAGE_REGISTER_COMPANY: {
    code: 2002,
    message: 'Compañía guardada correctamente.',
  },
  MESSAGE_REGISTER_USER: {
    code: 2003,
    message: 'Usuario guardado correctamente.',
  },
};

//Message delete code > 3000
export const DELETE = {
  MESSAGE_DELETE_CLIENT: {
    code: 3000,
    message: 'Cliente eliminado correctamente.',
  },
  MESSAGE_DELETE_PRODUCT: {
    code: 3001,
    message: 'Producto eliminado correctamente.',
  },
  MESSAGE_DELETE_COMPANY: {
    code: 3002,
    message: 'Compañía eliminada correctamente.',
  },
  MESSAGE_DELETE_USER: {
    code: 3003,
    message: 'Usuario eliminado correctamente.',
  },
};

//Message update code > 4000
export const UPDATE = {
  MESSAGE_UPDATE_CLIENT: {
    code: 4000,
    message: 'Cliente actualizado correctamente.',
  },
  MESSAGE_UPDATE_PRODUCT: {
    code: 4001,
    message: 'Producto actualizado correctamente.',
  },
  MESSAGE_UPDATE_COMPANY: {
    code: 4002,
    message: 'Compañía actualizada correctamente.',
  },
  MESSAGE_UPDATE_USER: {
    code: 4003,
    message: 'Usuario actualizado correctamente.',
  },
};

//Message header code > 5000
export const HEADER_MESSAGE = {
  MESSAGE_HEADER_INFO: {
    code: 5000,
    message: 'Info',
  },
  MESSAGE_HEADER_ERROR: {
    code: 5001,
    message: 'Error',
  },
};

export const EMPTY_DATA = {
  MESSAGE_EMPTY_DATA: {
    code: 6000,
    message: 'No existe datos para el filtro seleccionado.',
  },
  MESSAGE_EMPTY: {
    code: 6001,
    message: 'No existe datos',
  },
};

export const EXIST_DATA = {
  MESSAGE_EXISTS_IDENTIFICATION: {
    code: 7000,
    message: 'La identificación ingresada ya se encuentra registrada.',
  },
  MESSAGE_EXISTS_MAINCODE: {
    code: 7001,
    message: 'El código principal ingresado ya se encuentra registrado.',
  },
  MESSAGE_EXISTS_USER: {
    code: 7002,
    message: 'El usuario ingresado ya se encuentra registrado.',
  },
  MESSAGE_EXISTS_MAIL: {
    code: 7003,
    message: 'El correo ingresado ya se encuentra registrado.',
  },
};

export const PASSWORD_INCORRECT = {
  MESSAGE_PASSWORD_INCORRECT: {
    code: 8000,
    message: 'La contraseña actual ingresada no es correcta.',
  },
};

//Use http code
export const MESSAGE_ERROR_SERVER = {
  MESSAGE_ERROR_SERVER_400: {
    code: 400,
    message: 'Error 400',
  },
  MESSAGE_ERROR_SERVER_401: {
    code: 401,
    message: 'Error 401',
  },
  MESSAGE_ERROR_SERVER_404: {
    code: 404,
    message: 'Error 404',
  },
  MESSAGE_ERROR_SERVER_429: {
    code: 429,
    message: 'Too Many Requests',
  },
  MESSAGE_ERROR_SERVER_500: {
    code: 500,
    message: 'Error 500',
  },
  MESSAGE_ERROR_SERVER_0: {
    code: 0,
    message: 'Unknown Error',
  },
};

//=========================CATALOGOS CLIENTES===========================
export const CLIENT_TYPE = [
  { code: 1, value: 'Cliente' },
  { code: 2, value: 'Sujeto' },
  { code: 3, value: 'Destinatario' },
];

// Tipo de cliente por defecto para facturación electrónica
export const DEFAULT_CLIENT_TYPE_CODE = 1; // Cliente

// Tabla de valores según especificación del backend:
// CÉDULA = 1, RUC = 2, PASAPORTE = 3, CONSUMIDOR FINAL = 4, EXTERIOR = 5
export const IDENTIFICATION_TYPE = [
  { code: 1, value: 'Cédula' }, // idType: 1, Longitud: 10 dígitos
  { code: 2, value: 'RUC' }, // idType: 2, Longitud: 13 dígitos
  { code: 3, value: 'Pasaporte' }, // idType: 3, Alfanumérico, máx 10 caracteres
  { code: 4, value: 'Consumidor final' }, // idType: 4, Valor fijo: 9999999999999
  { code: 5, value: 'Identificación del exterior' }, // idType: 5, Alfanumérico, hasta 13 caracteres
];

// Constantes para validación de identificaciones
export const ID_TYPE_CEDULA = 1;
export const ID_TYPE_RUC = 2;
export const ID_TYPE_PASAPORTE = 3;
export const ID_TYPE_CONSUMIDOR_FINAL = 4;
export const ID_TYPE_EXTERIOR = 5;

export const CONSUMIDOR_FINAL_VALUE = '9999999999999'; // Valor fijo para consumidor final
export const CONSUMIDOR_FINAL_NAME = 'CONSUMIDOR FINAL'; // Nombre fijo para consumidor final

export const CONTACT_TYPE_PHONE = {
  code: 'TEL',
  value: 'Teléfono',
};
export const CONTACT_TYPE_MOBIL = {
  code: 'CEL',
  value: 'Celular',
};
export const CONTACT_TYPE_MAIL = {
  code: 'MAI',
  value: 'Mail',
};
export const CONTACT_TYPE_ADRESS = {
  code: 'DIR',
  value: 'Dirección',
};
export const IDENTIFICATION_TYPE_RUC_LENGTH = {
  code: 'RUC',
  value: 13,
};
export const IDENTIFICATION_TYPE_CEDULA_LENGTH = {
  code: 'CEDULA',
  value: 10,
};
export const IDENTIFICATION_TYPE_PASSPORT_LENGTH = {
  code: 'PASS',
  value: 20,
};
export const IDENTIFICATION_TYPE_DEFAULT_LENGTH = {
  code: 'DEFAULT',
  value: 5,
};

//=========================CATALOGOS PRODUCTOS===========================
export const PRODUCT_TYPE = [
  { code: 1, value: 'Bien' },
  { code: 2, value: 'Servicio' },
];

//=========================TIEMPO DE DURACION DE MENSAJE=================
export const DURATION_TIME_MESSAGE = { code: 1, value: 5000 };

//========================= ROLES =======================================
export const SUPER_ADMIN_ROLE = {
  name: 'super_admin',
  description: 'Super Admin',
};
export const ADMIN_ROLE = { name: 'admin', description: 'Administrador' };

//=========================CATALOGOS GUÍAS DE REMISIÓN===========================

/**
 * Tipos de emisión para guías de remisión
 */
export const GUIDE_EMISSION_TYPE = [
  { code: 'FISICA', value: 'Física' },
  { code: 'ELECTRONICA', value: 'Electrónica' },
];

/**
 * Tipos de documentos de sustento para guías de remisión
 * Basado en los códigos del SRI
 */
export const GUIDE_DOCUMENT_TYPE = [
  {
    code: '01',
    value: 'Factura',
    description:
      'Es el más común. Se usa cuando el traslado es por una venta ya realizada.',
  },
  {
    code: '03',
    value: 'Liquidación de Compra',
    description:
      'Cuando compras a alguien que no tiene RUC (artesanos, agricultores) y tú emites el documento.',
  },
  {
    code: '04',
    value: 'Nota de Crédito',
    description:
      'Se usa cuando el motivo es una devolución de mercadería por parte del cliente.',
  },
  {
    code: '05',
    value: 'Nota de Débito',
    description:
      'Menos común en traslados, pero se usa si hay un ajuste de precio/cantidad que afecte el envío.',
  },
  {
    code: '41',
    value: 'Comprobante de Retención',
    description: 'En casos específicos donde la ley lo permite como sustento.',
  },
  {
    code: 'DAI',
    value: 'Declaración Aduanera (DAI/DAE)',
    description:
      'Para importaciones o exportaciones (se suele poner el número de la declaración).',
  },
];

/**
 * Tipos de búsqueda para comprobante de sustento
 */
export const GUIDE_SEARCH_BY_TYPE = [
  { code: 'ACCESS_KEY', value: 'Clave de acceso' },
  { code: 'DOCUMENT_NUMBER', value: 'Número de documento' },
];

/**
 * Tipos de transporte para guías de remisión
 */
export const GUIDE_TRANSPORT_TYPE = [
  { code: 'Terrestre', value: 'Terrestre' },
  { code: 'Aéreo', value: 'Aéreo' },
  { code: 'Marítimo', value: 'Marítimo' },
  { code: 'Fluvial', value: 'Fluvial' },
];

/**
 * Motivos de traslado comunes para guías de remisión
 */
export const GUIDE_REASON_FOR_TRANSFER = [
  { code: 'Venta', value: 'Venta' },
  {
    code: 'Traslado entre establecimientos',
    value: 'Traslado entre establecimientos',
  },
  { code: 'Exportación', value: 'Exportación' },
  { code: 'Importación', value: 'Importación' },
  { code: 'Devolución', value: 'Devolución' },
  { code: 'Consignación', value: 'Consignación' },
  { code: 'Reparación', value: 'Reparación' },
  { code: 'Otro', value: 'Otro' },
];

/**
 * Códigos de identificación para transportistas
 * Según API: identificationType usa strings como "RUC", "CÉDULA", "PASAPORTE"
 */
export const CARRIER_IDENTIFICATION_TYPE = [
  { code: '2', value: 'RUC' },
  { code: '1', value: 'Cédula' },
  { code: '3', value: 'Pasaporte' },
];
