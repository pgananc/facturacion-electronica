/**
 * Utilidades para sanitizar y formatear mensajes de error
 *
 * Estas funciones filtran mensajes técnicos del backend (excepciones Java, stack traces, etc.)
 * y los reemplazan con mensajes amigables para el usuario final.
 */

/**
 * Detecta si un mensaje es técnico (excepción Java, stack trace, etc.)
 */
function isTechnicalMessage(message: string): boolean {
  if (!message || typeof message !== 'string') {
    return false;
  }

  const technicalPatterns = [
    // Excepciones Java comunes
    /NullPointerException/i,
    /Cannot invoke/i,
    /because the return value of/i,
    /is null/i,
    /java\./i,
    /com\./i,
    /org\./i,
    /at \w+\.\w+\(/i, // Stack traces: "at Class.method("
    /Caused by:/i,
    /Exception in thread/i,
    /\.get[A-Z]\w+\(\)/i, // Métodos getter: ".getRate()", ".getProductDto()"
    /\.set[A-Z]\w+\(\)/i, // Métodos setter
    /\.\w+\(\)/i, // Cualquier método con paréntesis seguido de "is null"
    /return value of.*is null/i,
    // Mensajes técnicos específicos
    /Cannot invoke ".*" because/i,
    /because.*is null/i,
    /\.get[A-Z]\w+\(\)/i,
    // Errores de índices y arrays
    /Index \d+ out of bounds/i, // "Index 0 out of bounds for length 0"
    /out of bounds/i, // Cualquier error de índices fuera de rango
    /ArrayIndexOutOfBoundsException/i,
    /IndexOutOfBoundsException/i,
    // Errores de Java/Spring Boot específicos
    /class java\./i, // "class java.lang..."
    /in module java\.base/i, // "in module java.base of loader 'bootstrap'"
    /of loader/i, // "of loader 'bootstrap'"
    /Error al generar/i, // "Error al generar el dashboard: class java..."
    /Error al.*: class/i, // Patrón común de errores del backend
    // Errores SQL y de base de datos
    /could not execute statement/i, // "could not execute statement"
    /syntax error at or near/i, // "syntax error at or near"
    /SQL \[.*\]/i, // "SQL [insert into...]"
    /insert into.*values/i, // Queries SQL completas
    /update.*set/i, // Queries UPDATE
    /delete from/i, // Queries DELETE
    /select.*from/i, // Queries SELECT (solo si están en mensajes de error)
    /ERROR:\s*syntax error/i, // "ERROR: syntax error"
    /Position:\s*\d+/i, // "Position: 49"
    /\[ERROR:.*\]/i, // "[ERROR: ...]"
    /violates.*constraint/i, // "violates foreign key constraint"
    /duplicate key value/i, // "duplicate key value"
    /column.*does not exist/i, // "column does not exist"
    /relation.*does not exist/i, // "relation does not exist"
    // Mensajes técnicos de Angular/HTTP
    /Http failure response for/i,
    /http:\/\/localhost:\d+\/api\//i, // URLs locales
    /https?:\/\/.*\/api\//i, // URLs de API
    /Internal Server Error/i,
    /Bad Request/i,
    /Unauthorized/i,
    /Forbidden/i,
    /Not Found/i,
    /\d{3} [A-Z][a-z]+ [A-Z][a-z]+ [A-Z][a-z]+/i, // "500 Internal Server Error"
    /status code/i,
    /status:\s*\d+/i,
  ];

  return technicalPatterns.some((pattern) => pattern.test(message));
}

/**
 * Sanitiza un mensaje de error, reemplazando mensajes técnicos con mensajes amigables
 *
 * @param message - Mensaje de error original
 * @param context - Contexto de la operación (ej: "crear nota de crédito", "guardar factura")
 * @returns Mensaje sanitizado y amigable
 */
export function sanitizeErrorMessage(
  message: string | null | undefined,
  context: string = 'realizar la operación'
): string {
  if (!message || typeof message !== 'string') {
    return `Error al ${context}. Por favor, intenta nuevamente o contacta al administrador si el problema persiste.`;
  }

  // Si el mensaje es técnico, reemplazarlo con uno amigable
  if (isTechnicalMessage(message)) {
    // Intentar extraer información útil del mensaje técnico
    const productMatch = message.match(/ProductDto|Product|producto/i);
    const fieldMatch = message.match(/get(\w+)\(\)/i);

    if (productMatch && fieldMatch) {
      const fieldName = fieldMatch[1]?.toLowerCase() || 'información';
      return `Error al procesar los datos del producto. Verifica que todos los campos estén completos e intenta nuevamente.`;
    }

    // Mensaje genérico para errores técnicos
    return `Error al ${context}. Por favor, verifica que todos los datos estén completos e intenta nuevamente. Si el problema persiste, contacta al administrador.`;
  }

  // Si el mensaje contiene "default message [...]" (formato Spring Boot), extraerlo
  const defaultMessageMatch = message.match(/default message \[([^\]]+)\]/g);
  if (defaultMessageMatch && defaultMessageMatch.length > 0) {
    const lastMatch = defaultMessageMatch[defaultMessageMatch.length - 1];
    const extracted = lastMatch.match(/default message \[([^\]]+)\]/);
    if (extracted && extracted[1]) {
      const extractedMessage = extracted[1];
      // Verificar si el mensaje extraído también es técnico
      if (!isTechnicalMessage(extractedMessage)) {
        return extractedMessage;
      }
    }
  }

  // Si el mensaje no es técnico, retornarlo tal cual
  return message.trim();
}

/**
 * Extrae un mensaje de error amigable de una respuesta HTTP de error
 *
 * @param error - Objeto HttpErrorResponse o error genérico
 * @param context - Contexto de la operación
 * @returns Mensaje de error amigable
 */
export function extractUserFriendlyErrorMessage(
  error: any,
  context: string = 'realizar la operación'
): string {
  // 1. Intentar obtener mensaje de error.error.message
  if (error?.error?.message) {
    return sanitizeErrorMessage(error.error.message, context);
  }

  // 1.1. Si es error 429 y no hay mensaje, puede ser bloqueado por CORS
  // Intentar extraer información de otros campos del error
  if (error?.status === 429) {
    // Intentar obtener mensaje de diferentes ubicaciones posibles
    const possibleMessage =
      error?.error?.message ||
      error?.error?.error ||
      (typeof error?.error === 'string' ? error.error : null) ||
      error?.message;

    // Si encontramos un mensaje que no sea técnico, usarlo
    if (possibleMessage && !isTechnicalMessage(String(possibleMessage))) {
      return sanitizeErrorMessage(possibleMessage, context);
    }
  }

  // 2. Intentar obtener mensaje de error.error.errors (array de errores de validación)
  if (error?.error?.errors && Array.isArray(error.error.errors)) {
    const messages = error.error.errors
      .map((e: any) => e.message || e.defaultMessage || String(e))
      .filter((msg: string) => msg && !isTechnicalMessage(String(msg)))
      .map((msg: string) => sanitizeErrorMessage(msg, context));

    if (messages.length > 0) {
      return messages.join(', ');
    }
  }

  // 3. Intentar obtener mensaje de error.error (si es string)
  if (typeof error?.error === 'string') {
    return sanitizeErrorMessage(error.error, context);
  }

  // 4. Intentar obtener mensaje de error.message (puede contener "Http failure response...")
  if (error?.message) {
    // Si el mensaje es técnico de Angular (contiene "Http failure response"), ignorarlo
    if (isTechnicalMessage(error.message)) {
      // No usar el mensaje técnico, continuar al switch de códigos HTTP
    } else {
      return sanitizeErrorMessage(error.message, context);
    }
  }

  // 5. Mensaje por defecto según código de estado HTTP
  const status = error?.status;
  switch (status) {
    case 400:
      return `Datos inválidos. Por favor, verifica la información ingresada e intenta nuevamente.`;
    case 401:
      return `Tu sesión ha expirado. Por favor, inicia sesión nuevamente.`;
    case 403:
      return `No tienes permisos para realizar esta acción. Contacta al administrador.`;
    case 404:
      return `El recurso solicitado no fue encontrado. Verifica la información e intenta nuevamente.`;
    case 409:
      return `Ya existe un registro con esta información. Verifica los datos e intenta nuevamente.`;
    case 429:
      // Si hay un mensaje del backend, ya se extrajo arriba
      // Este es el fallback si no se encontró mensaje específico
      return `Se ha excedido el límite de solicitudes. Por favor, espera unos momentos e intenta nuevamente.`;
    case 500:
      return `Error al ${context}. Por favor, intenta nuevamente en unos momentos. Si el problema persiste, contacta al administrador.`;
    case 503:
      return `Servicio temporalmente no disponible. Por favor, intenta más tarde.`;
    default:
      return `Error al ${context}. Por favor, intenta nuevamente o contacta al administrador si el problema persiste.`;
  }
}
