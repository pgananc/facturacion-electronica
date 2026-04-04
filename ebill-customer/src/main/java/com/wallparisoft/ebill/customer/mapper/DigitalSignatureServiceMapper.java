package com.wallparisoft.ebill.customer.mapper;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.customer.dto.DigitalSignatureDto;
import com.wallparisoft.ebill.customer.entity.DigitalSignature;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@FieldDefaults(level = PRIVATE)
public class DigitalSignatureServiceMapper {

  public List<DigitalSignatureDto> toDigitalSignatureDtos(
      List<DigitalSignature> digitalSignatures) {
    List<DigitalSignatureDto> digitalSignatureDtos =
        digitalSignatures.stream()
            .map(
                digitalSignature ->
                    DigitalSignatureDto.builder()
                        .password(digitalSignature.getPassword())
                        .signFile(digitalSignature.getSignFile())
                        .build())
            .collect(Collectors.toList());

    return digitalSignatureDtos;
  }

  public DigitalSignatureDto toDigitalSignatureDto(DigitalSignature digitalSignature) {
    DigitalSignatureDto digitalSignatureDto =
        DigitalSignatureDto.builder()
            .id(digitalSignature.getId())
            .password(digitalSignature.getPassword())
            .signFile(digitalSignature.getSignFile())
            .fileName(digitalSignature.getFileName())
            .build();

    return digitalSignatureDto;
  }
}
