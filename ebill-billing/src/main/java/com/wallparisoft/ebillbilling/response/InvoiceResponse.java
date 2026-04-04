package com.wallparisoft.ebillbilling.response;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = PRIVATE)
public class InvoiceResponse extends BasicResponse {

  List<InvoiceDto> invoiceDtos;
  InvoiceDto invoiceDto;
  Long idInvoice;
  private String accessKey;
  private String invoiceNumber;
}
