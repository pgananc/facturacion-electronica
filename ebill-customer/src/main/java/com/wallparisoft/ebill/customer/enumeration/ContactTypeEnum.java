package com.wallparisoft.ebill.customer.enumeration;

public enum ContactTypeEnum {
    TEL("Teléfono"), CEL("Celular"), MAIL("Mail"), DIR("Dirección");
    private String description;

    private ContactTypeEnum(final String description) {
        this.description = description;

    }
}
