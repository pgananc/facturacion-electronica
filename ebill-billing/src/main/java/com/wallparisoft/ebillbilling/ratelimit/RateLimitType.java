package com.wallparisoft.ebillbilling.ratelimit;

/** Tipo de rate limiting a aplicar según el endpoint. */
public enum RateLimitType {
  /** Para endpoints de consulta como /api/invoice/documents/query */
  QUERY,
  /** Para endpoints de descarga como /api/invoice/{id}/pdf y /api/invoice/{id}/xml */
  DOWNLOAD
}
