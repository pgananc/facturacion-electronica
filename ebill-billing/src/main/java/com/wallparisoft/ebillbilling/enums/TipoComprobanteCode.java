package com.wallparisoft.ebillbilling.enums;

/**
 * Códigos de tipos de comprobantes según el catálogo del SRI
 *
 * @see <a href="https://www.sri.gob.ec/catalogo-de-tipos-de-comprobantes">Catálogo SRI</a>
 */
public enum TipoComprobanteCode {

  /** Factura - Código SRI: 01 */
  FACTURA("01", "Factura"),

  /** Liquidación de compra de bienes y prestación de servicios - Código SRI: 03 */
  LIQUIDACION_COMPRA("03", "Liquidación de compra"),

  /** Nota de crédito - Código SRI: 04 */
  NOTA_CREDITO("04", "Nota de crédito"),

  /** Nota de débito - Código SRI: 05 */
  NOTA_DEBITO("05", "Nota de débito"),

  /** Guía de remisión - Código SRI: 06 */
  GUIA_REMISION("06", "Guía de remisión"),

  /** Comprobante de retención - Código SRI: 07 */
  COMPROBANTE_RETENCION("07", "Comprobante de retención");

  private final String code;
  private final String description;

  TipoComprobanteCode(String code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Obtiene el código del tipo de comprobante (ej: "01")
   *
   * @return Código del SRI
   */
  public String getCode() {
    return code;
  }

  /**
   * Obtiene la descripción del tipo de comprobante
   *
   * @return Descripción legible
   */
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return code + " - " + description;
  }
}
