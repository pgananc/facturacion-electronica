package com.wallparisoft.ebill.utils.enums;

import lombok.Data;


public enum ParmsAuthEnum {

    COD001_MAIL_TEMPLATE_RESTORE_PASSWORD("MAIL_REST_PASS"),
    COD002_TIME_DURATION_TOKEN("TIME_DURATION_TOKEN"),
    COD003_URL_REST_PASS("URL_REST_PASS");

    private String code;


    private ParmsAuthEnum(String code){
        this.code= code;

    }

    public String getCode() {
        return code;
    }
}
