package com.wallparisoft.ebillbilling.dto.proforma;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProformaSearchRequestDto {

  @NotNull(message = "idCompany is required")
  Long idCompany;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate issueDateFrom;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  LocalDate issueDateTo;

  /** DRAFT, SENT, CONVERTED, EXPIRED */
  String status;

  Long idClient;

  String clientIdentification;

  Integer page;
  Integer size;
  String sort; // e.g. "issueDate,desc"
}
