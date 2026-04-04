package com.wallparisoft.ebill.product.mapper;

import com.wallparisoft.ebill.product.dto.TaxTypeDto;
import com.wallparisoft.ebill.product.entity.TaxType;
import com.wallparisoft.ebill.product.response.TaxTypeResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaxTypeMapper {

  TaxTypeDto toTaxTypeDto(TaxType taxType);

  TaxType toTaxType(TaxTypeDto taxTypeDto);

  List<TaxTypeDto> toTaxTypeDtoList(List<TaxType> taxTypes);

  default TaxTypeResponse toTaxTypeResponse(List<TaxType> taxTypes) {
    return TaxTypeResponse.builder().taxTypes(toTaxTypeDtoList(taxTypes)).build();
  }

  default TaxTypeResponse toTaxTypeResponse(TaxType taxType) {
    return TaxTypeResponse.builder().taxType(toTaxTypeDto(taxType)).build();
  }
}
