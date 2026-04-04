package com.wallparisoft.ebillbilling.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CanEmitRequestDto {
  private String documentType; // INVOICE, CREDIT_NOTE, DEBIT_NOTE, RETENTION, GUIDE
}
