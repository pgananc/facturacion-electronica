package com.wallparisoft.ebill.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClientSearchRequestDto implements Serializable {

  @NotBlank(message = "Identification is required")
  String identification;

  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  String email;
}
