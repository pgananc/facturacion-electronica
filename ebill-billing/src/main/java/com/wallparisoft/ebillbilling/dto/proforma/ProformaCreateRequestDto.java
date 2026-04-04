package com.wallparisoft.ebillbilling.dto.proforma;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
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
public class ProformaCreateRequestDto {

  @NotNull(message = "idCompany is required")
  Long idCompany;

  @NotNull(message = "idEstablishment is required")
  Long idEstablishment;

  @NotNull(message = "idIssuancePoint is required")
  Long idIssuancePoint;

  Long idClient;

  @NotNull(message = "issueDate is required")
  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate issueDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate validUntil;

  @Size(max = 500)
  String additionalInformation;

  @NotNull(message = "details is required")
  @Valid
  @Size(min = 1, message = "at least one detail line is required")
  List<ProformaDetailRequestDto> details;
}
