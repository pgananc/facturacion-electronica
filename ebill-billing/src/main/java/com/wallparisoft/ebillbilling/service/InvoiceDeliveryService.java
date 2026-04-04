package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Invoice;

public interface InvoiceDeliveryService {

  void deliverAuthorizedInvoice(
      InvoiceDto invoiceDto, Invoice invoice, AuthorizationSri authorizationSri);
}
