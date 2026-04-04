package com.wallparisoft.ebill.customer.mapper;

import com.wallparisoft.ebill.customer.dto.EstablishmentDto;
import com.wallparisoft.ebill.customer.dto.EstablishmentRequestDto;
import com.wallparisoft.ebill.customer.dto.EstablishmentResponseDto;
import com.wallparisoft.ebill.customer.entity.Establishment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(
    componentModel = "spring",
    uses = {InvoiceIssuancePointMapper.class})
public interface EstablishmentMapper {

  // DTO completo con puntos de emisión
  @Mapping(source = "company.idCompany", target = "companyId")
  EstablishmentResponseDto toDto(Establishment entity);

  // DTO ligero sin puntos de emisión
  @Mappings({
    @Mapping(source = "company.idCompany", target = "companyId"),
    @Mapping(target = "invoiceIssuancePoints", ignore = true)
  })
  EstablishmentResponseDto toDtoWithoutinvoiceIssuancePoints(Establishment entity);

  // (opcional) Si necesitas el toEntity
  @Mapping(source = "companyId", target = "company.idCompany")
  Establishment toEntity(EstablishmentResponseDto dto);

  @Mapping(source = "companyId", target = "company.idCompany")
  Establishment toEntity(EstablishmentRequestDto dto);

  EstablishmentDto toSimpleDto(Establishment entity);
}
