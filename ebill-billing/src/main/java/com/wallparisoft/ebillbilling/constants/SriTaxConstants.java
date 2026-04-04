package com.wallparisoft.ebillbilling.constants;

/**
 * Constantes para códigos de impuestos según el catálogo del SRI
 *
 * @see <a href="https://www.sri.gob.ec/impuestos">Catálogo de Impuestos SRI</a>
 */
public final class SriTaxConstants {

  private SriTaxConstants() {
    throw new UnsupportedOperationException(
        "Esta es una clase de constantes y no debe ser instanciada");
  }

  // ===== CÓDIGOS DE IMPUESTOS =====

  /** Código del IVA (Impuesto al Valor Agregado) */
  public static final String CODE_IVA = "2";

  /** Código del ICE (Impuesto a los Consumos Especiales) */
  public static final String CODE_ICE = "3";

  /** Código del IRBPNR (Impuesto a los Redimibles Plásticos de Uso) */
  public static final String CODE_IRBPNR = "5";

  // ===== CÓDIGOS DE PORCENTAJE ICE =====

  /**
   * Código porcentaje ICE - 3072 Usado para productos con ICE específico
   *
   * <p>NOTA: Este código puede variar según el tipo de producto Verificar con el catálogo
   * actualizado del SRI
   */
  public static final String ICE_PERCENTAGE_CODE_3072 = "3072";

  /** Código porcentaje ICE - 3073 Usado para bebidas alcohólicas */
  public static final String ICE_PERCENTAGE_CODE_3073 = "3073";

  // ===== TARIFAS ICE =====

  /**
   * Tarifa ICE del 5% NOTA: La tarifa ICE depende del tipo de producto Verificar con el catálogo
   * actualizado del SRI
   */
  public static final String ICE_RATE_5_PERCENT = "5";

  /** Tarifa ICE del 10% */
  public static final String ICE_RATE_10_PERCENT = "10";

  /** Tarifa ICE del 15% */
  public static final String ICE_RATE_15_PERCENT = "15";

  /** Tarifa ICE del 20% */
  public static final String ICE_RATE_20_PERCENT = "20";

  /** Tarifa ICE del 25% */
  public static final String ICE_RATE_25_PERCENT = "25";

  /** Tarifa ICE del 30% */
  public static final String ICE_RATE_30_PERCENT = "30";

  /** Tarifa ICE del 35% */
  public static final String ICE_RATE_35_PERCENT = "35";

  /** Tarifa ICE del 75% */
  public static final String ICE_RATE_75_PERCENT = "75";

  /** Tarifa ICE del 100% */
  public static final String ICE_RATE_100_PERCENT = "100";

  /** Tarifa ICE del 300% */
  public static final String ICE_RATE_300_PERCENT = "300";

  // ===== VALORES POR DEFECTO =====

  /** Código porcentaje ICE por defecto si no está especificado */
  public static final String DEFAULT_ICE_PERCENTAGE_CODE = ICE_PERCENTAGE_CODE_3072;

  /** Tarifa ICE por defecto si no está especificada */
  public static final String DEFAULT_ICE_RATE = ICE_RATE_5_PERCENT;
}
