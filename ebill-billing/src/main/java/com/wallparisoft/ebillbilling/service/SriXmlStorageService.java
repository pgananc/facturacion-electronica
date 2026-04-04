package com.wallparisoft.ebillbilling.service;

import java.nio.file.Path;
import java.time.LocalDateTime;

public interface SriXmlStorageService {

  Path storeAuthorizationXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey);

  Path storeCreditNoteXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey);

  Path storeDebitNoteXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey);

  Path storeGuideXml(
      String xmlContent,
      String buyerIdentification,
      LocalDateTime authorizationDate,
      String accessKey);
}
