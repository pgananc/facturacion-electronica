package com.wallparisoft.ebillbilling.service;

import java.nio.file.Path;

public interface InvoicePdfService {

  public Path generateAndSaveInvoicePdf(String authorizationXml, Path outputDirectory);

  public Path generateAndSaveInvoicePdf(
      String authorizationXml, Path outputDirectory, String logoPath);

  public Path generateAndSaveInvoicePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution);
}
