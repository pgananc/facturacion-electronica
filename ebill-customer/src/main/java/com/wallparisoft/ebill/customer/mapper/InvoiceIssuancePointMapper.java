package com.wallparisoft.ebill.customer.mapper;

import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointRequestDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointResponseDto;
import com.wallparisoft.ebill.customer.entity.InvoiceIssuancePoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceIssuancePointMapper {

  @Mapping(source = "establishment.idEstablishment", target = "establishmentId")
  InvoiceIssuancePointResponseDto toDto(InvoiceIssuancePoint entity);

  @Mapping(source = "establishmentId", target = "establishment.idEstablishment")
  InvoiceIssuancePoint toEntity(InvoiceIssuancePointRequestDto dto);
}
