package com.wallparisoft.ebillbilling.service;

import java.nio.file.Path;

public interface IInvoiceFileService {

  /**
   * Obtiene el contenido del archivo XML de una factura autorizada.
   *
   * @param invoiceId ID de la factura
   * @return Bytes del archivo XML
   */
  byte[] getXmlFile(Long invoiceId);

  /**
   * Obtiene la ruta del archivo XML (para streaming).
   *
   * @param invoiceId ID de la factura
   * @return Path del archivo XML
   */
  Path getXmlFilePath(Long invoiceId);

  /**
   * Obtiene el contenido del archivo PDF de una factura autorizada.
   *
   * @param invoiceId ID de la factura
   * @return Bytes del archivo PDF
   */
  byte[] getPdfFile(Long invoiceId);

  /**
   * Obtiene la ruta del archivo PDF (para streaming).
   *
   * @param invoiceId ID de la factura
   * @return Path del archivo PDF
   */
  Path getPdfFilePath(Long invoiceId);

  /**
   * Obtiene el nombre del archivo XML para descarga.
   *
   * @param invoiceId ID de la factura
   * @return Nombre del archivo XML
   */
  String getXmlFileName(Long invoiceId);

  /**
   * Obtiene el nombre del archivo PDF para descarga.
   *
   * @param invoiceId ID de la factura
   * @return Nombre del archivo PDF
   */
  String getPdfFileName(Long invoiceId);
}
