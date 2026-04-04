package com.wallparisoft.ebillbilling.service;

import java.nio.file.Path;

public interface GuidePdfService {

  public Path generateAndSaveGuidePdf(String authorizationXml, Path outputDirectory);

  public Path generateAndSaveGuidePdf(
      String authorizationXml, Path outputDirectory, String logoPath);

  public Path generateAndSaveGuidePdf(
      String authorizationXml,
      Path outputDirectory,
      String logoPath,
      String regime,
      String specialTaxpayerResolution);
}
