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
  MESSAGE_ERROR_SERVER_500: {
    code: 500,
    message: 'Error 500',
  },
  MESSAGE_ERROR_SERVER_404: {
    code: 404,
    message: 'Error 404',
  },
};

//=========================CATALOGOS CLIENTES===========================
export const CLIENT_TYPE = [
  { code: 1, value: 'Cliente' },
  { code: 2, value: 'Sujeto' },
  { code: 3, value: 'Destinatario' },
];
export const IDENTIFICATION_TYPE = [
  { code: 1, value: 'RUC' },
  { code: 2, value: 'Cedula' },
  { code: 3, value: 'Pasaporte' },
];

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

//=========================TIEMPO DE DURACION DE MENSAJE===========================
export const DURATION_TIME_MESSAGE = { code: 1, value: 5000 };
