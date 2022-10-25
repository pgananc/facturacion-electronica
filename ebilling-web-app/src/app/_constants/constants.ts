import { EMPTY } from 'rxjs';
//Message general code > -1000
export const ERRORS = {
  MESSAGE_ERROR_REGISTER: {
    code: -1000,
    message: 'Error al procesar registro.',
  },
  MESSAGE_ERROR_NOT_AVAILABLE: {
    code: -1001,
    message: 'Servicio no disponible intente más tarde',
  },
  MESSAGE_ERROR: {
    code: -1002,
    message: 'Ha ocurrido un error intente más tarde',
  },
};

//Message success code > 2000
export const SUCCESS = {
  MESSAGE_REGISTER_CLIENT: {
    code: 2000,
    message: 'Cliente guardado correctamente.',
  },
};

//Message delete code > 3000
export const DELETE = {
  MESSAGE_DELETE_CLIENT: {
    code: 3000,
    message: 'Cliente eliminado correctamente.',
  },
};

//Message update code > 4000
export const UPDATE = {
  MESSAGE_UPDATE_CLIENT: {
    code: 4000,
    message: 'Cliente actualizado correctamente.',
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
  MESSAGE_EXIST_DATA: {
    code: 7000,
    message: 'La identificación ingresada ya se encuentra regitrada.',
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
  value: 'Teléfono',
};
export const IDENTIFICATION_TYPE_RUC_LENGTH = {
  code: 'RUC',
  value: 13,
};
export const IDENTIFICATION_TYPE_CEDULA_LENGTH = {
  code: 'CEDULA',
  value: 10,
};
export const IDENTIFICATION_TYPE_PASSPORTE_LENGTH = {
  code: 'PASS',
  value: 20,
};
export const IDENTIFICATION_TYPE_DEFAULT_LENGTH = {
  code: 'DEFAULT',
  value: 5,
};
