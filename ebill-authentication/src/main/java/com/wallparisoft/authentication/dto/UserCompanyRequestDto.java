package com.wallparisoft.authentication.dto;

import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCompanyRequestDto implements Serializable {
  Long idCompany;
  Long idUser;
}
