package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.entity.Proforma;

public interface ProformaPdfService {

  /**
   * Genera el PDF para la proforma indicada.
   *
   * @param proforma entidad proforma con sus detalles cargados
   * @return contenido del PDF en un arreglo de bytes
   */
  byte[] generatePdf(Proforma proforma);
}
