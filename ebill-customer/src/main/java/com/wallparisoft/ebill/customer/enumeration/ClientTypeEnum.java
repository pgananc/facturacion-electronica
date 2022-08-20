package com.wallparisoft.ebill.customer.enumeration;

public enum ClientTypeEnum {
    CLIENTE(1, "Teléfono"), SUJETO(2, "Sujeto Retenido"), DESTINATARIO(3, "Destinatario");

    private Integer code;
    private String description;

    private ClientTypeEnum(final Integer code, final String description) {
        this.code = code;
        this.description = description;

    }
}
