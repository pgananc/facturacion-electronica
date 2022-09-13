package com.wallparisoft.ebill.product.enumeration;

public enum ProductTypeEnum {
    GOOD(1, "Bien"), SERVICE(2, "Servicio");

    private Integer code;
    private String description;

    private ProductTypeEnum(final Integer code, final String description) {
        this.code = code;
        this.description = description;

    }
}
