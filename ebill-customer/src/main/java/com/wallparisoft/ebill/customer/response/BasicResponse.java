package com.wallparisoft.ebill.customer.response;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BasicResponse {
    private int code;
    private String status;
    private String message;

}
