package com.wallparisoft.ebillbilling.dto.proforma;

import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProformaConvertToInvoiceResponseDto {

  Long idInvoice;
  String invoiceNumber;

  /** Full invoice draft payload for frontend to open the form pre-filled (optional). */
  InvoiceDto invoiceDto;
}
