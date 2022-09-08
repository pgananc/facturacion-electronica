package com.wallparisoft.ebill.customer.response;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class BasicResponse {
    private int code; //200 //
    private String status; //OK
    private String message;

}
