package com.wallparisoft.ebillbilling.service;

import java.nio.file.Path;

public interface CreditNotePdfService {

  public Path generateAndSaveCreditNotePdf(String authorizationXml, Path outputDirectory);

  public Path generateAndSaveCreditNotePdf(
      String authorizationXml, Path outputDirectory, String logoPath);

  public Path generateAndSaveCreditNotePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution);
}
