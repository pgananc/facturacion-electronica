package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.DigitalSignatureDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.security.cert.CertificateException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public interface ISendInvoiceService {

  public AuthorizationSri sendInvoiceSri(
      InvoiceDto invoiceDto,
      DigitalSignatureDto digitalSignatureDto,
      AuthorizationSri existingAuthorizationSri)
      throws SAXParseException,
          CertificateException,
          SAXException,
          IOException,
          JAXBException,
          InterruptedException;
}
