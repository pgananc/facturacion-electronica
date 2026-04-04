package com.wallparisoft.ebillbilling.service;

import java.nio.file.Path;

public interface DebitNotePdfService {

  public Path generateAndSaveDebitNotePdf(String authorizationXml, Path outputDirectory);

  public Path generateAndSaveDebitNotePdf(
      String authorizationXml, Path outputDirectory, String logoPath);

  public Path generateAndSaveDebitNotePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution);
}
