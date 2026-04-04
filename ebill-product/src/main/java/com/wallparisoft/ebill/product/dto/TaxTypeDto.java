package com.wallparisoft.ebill.product.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO para TaxType usando Java Record. Jackson soporta records nativamente desde 2.12+ MapStruct
 * soporta records desde 1.4+
 */
public record TaxTypeDto(Long idTaxType, String type, String name, BigDecimal value, Boolean status)
    implements Serializable {}
