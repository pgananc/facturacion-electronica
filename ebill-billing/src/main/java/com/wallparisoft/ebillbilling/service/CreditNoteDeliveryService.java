package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.CreditNote;

public interface CreditNoteDeliveryService {

  void deliverAuthorizedCreditNote(
      CreditNoteDto creditNoteDto, CreditNote creditNote, AuthorizationSri authorizationSri);
}
