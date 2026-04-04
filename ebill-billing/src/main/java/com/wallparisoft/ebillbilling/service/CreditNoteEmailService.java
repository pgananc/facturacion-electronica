package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.CreditNote;

public interface CreditNoteEmailService {

  Integer sendAuthorizedCreditNote(
      String recipientEmail,
      CreditNote creditNote,
      CreditNoteDto creditNoteDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes);
}
