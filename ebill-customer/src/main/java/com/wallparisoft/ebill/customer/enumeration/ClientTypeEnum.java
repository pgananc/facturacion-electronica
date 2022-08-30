package com.wallparisoft.ebill.customer.enumeration;

public enum ClientTypeEnum {
    CLIENT(1, "Cliente"), SUBJET_RET(2, "Sujeto Retenido"), ADDRESSEE(3, "Destinatario");

    private Integer code;
    private String description;

    private ClientTypeEnum(final Integer code, final String description) {
        this.code = code;
        this.description = description;

    }
}
