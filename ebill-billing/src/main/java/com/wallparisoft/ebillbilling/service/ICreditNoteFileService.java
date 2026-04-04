package com.wallparisoft.ebillbilling.service;

import java.nio.file.Path;

public interface ICreditNoteFileService {

  /**
   * Obtiene el contenido del archivo XML de una nota de crédito autorizada.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Bytes del archivo XML
   */
  byte[] getXmlFile(Long creditNoteId);

  /**
   * Obtiene la ruta del archivo XML (para streaming).
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Path del archivo XML
   */
  Path getXmlFilePath(Long creditNoteId);

  /**
   * Obtiene el contenido del archivo PDF de una nota de crédito autorizada.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Bytes del archivo PDF
   */
  byte[] getPdfFile(Long creditNoteId);

  /**
   * Obtiene la ruta del archivo PDF (para streaming).
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Path del archivo PDF
   */
  Path getPdfFilePath(Long creditNoteId);

  /**
   * Obtiene el nombre del archivo XML para descarga.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Nombre del archivo XML
   */
  String getXmlFileName(Long creditNoteId);

  /**
   * Obtiene el nombre del archivo PDF para descarga.
   *
   * @param creditNoteId ID de la nota de crédito
   * @return Nombre del archivo PDF
   */
  String getPdfFileName(Long creditNoteId);
}
