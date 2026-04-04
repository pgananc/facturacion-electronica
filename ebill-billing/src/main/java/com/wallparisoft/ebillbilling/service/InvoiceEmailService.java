package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Invoice;

public interface InvoiceEmailService {

  Integer sendAuthorizedInvoice(
      String recipientEmail,
      Invoice invoice,
      InvoiceDto invoiceDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes);
}
