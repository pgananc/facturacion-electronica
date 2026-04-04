package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.DigitalSignatureDto;
import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.security.cert.CertificateException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public interface ISendGuideService {

  public AuthorizationSri sendGuideSri(
      GuideDto guideDto,
      DigitalSignatureDto digitalSignatureDto,
      AuthorizationSri existingAuthorizationSri)
      throws SAXParseException,
          CertificateException,
          SAXException,
          IOException,
          JAXBException,
          InterruptedException;
}
