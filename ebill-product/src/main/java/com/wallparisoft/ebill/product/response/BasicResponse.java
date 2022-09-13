package com.wallparisoft.ebill.product.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BasicResponse {
    private int code; //200 //
    private String status; //OK
    private String message;

}
