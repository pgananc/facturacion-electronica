package com.wallparisoft.ebill.customer.enumeration;

public enum IdentificationTypeEnum {
    RUC("RUC"), CED("Cedula"), PAS("Pasaporte");
    private String description;

    private IdentificationTypeEnum(final String description) {
        this.description = description;

    }
}
