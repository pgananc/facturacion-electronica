package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.DebitNote;

public interface DebitNoteEmailService {

  Integer sendAuthorizedDebitNote(
      String recipientEmail,
      DebitNote debitNote,
      DebitNoteDto debitNoteDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes);
}
