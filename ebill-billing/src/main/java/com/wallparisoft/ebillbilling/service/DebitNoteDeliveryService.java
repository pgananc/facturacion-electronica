package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.DebitNote;

public interface DebitNoteDeliveryService {

  void deliverAuthorizedDebitNote(
      DebitNoteDto debitNoteDto, DebitNote debitNote, AuthorizationSri authorizationSri);
}
