package com.wallparisoft.ebillbilling.service;

import java.util.Optional;

public interface ParamsService {

  Optional<String> getParamValue(String codeParam);

  String getParamValueOrDefault(String codeParam, String defaultValue);

  void updateParamValue(String codeParam, String value);

  /**
   * Obtiene el directorio base donde se almacenan los archivos relacionados a las facturas (XML y
   * PDF).
   *
   * <p>Se obtiene desde el parámetro {@code INVOICE_STORAGE_BASE_DIR} en la tabla params. Si no
   * existe en BD, se usa un valor por defecto.
   *
   * @return Ruta del directorio base de almacenamiento
   */
  String getBaseDir();

  /**
   * Obtiene el directorio base donde se almacenan los archivos relacionados a las notas de crédito
   * (XML y PDF).
   *
   * <p>Se obtiene desde el parámetro {@code CREDIT_NOTE_STORAGE_BASE_DIR} en la tabla params. Si no
   * existe en BD, se usa un valor por defecto.
   *
   * @return Ruta del directorio base de almacenamiento para notas de crédito
   */
  String getCreditNoteBaseDir();

  /**
   * Obtiene el directorio base donde se almacenan los archivos relacionados a las notas de débito
   * (XML y PDF).
   *
   * <p>Se obtiene desde el parámetro {@code DEBIT_NOTE_STORAGE_BASE_DIR} en la tabla params. Si no
   * existe en BD, se usa un valor por defecto.
   *
   * @return Ruta del directorio base de almacenamiento para notas de débito
   */
  String getDebitNoteBaseDir();

  /**
   * Obtiene el directorio base donde se almacenan los archivos relacionados a las guías de remisión
   * (XML y PDF).
   *
   * <p>Se obtiene desde el parámetro {@code GUIDE_STORAGE_BASE_DIR} en la tabla params. Si no
   * existe en BD, se usa un valor por defecto.
   *
   * @return Ruta del directorio base de almacenamiento para guías de remisión
   */
  String getGuideBaseDir();
}
