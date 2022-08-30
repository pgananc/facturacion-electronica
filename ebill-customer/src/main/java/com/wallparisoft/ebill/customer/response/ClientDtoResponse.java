package com.wallparisoft.ebill.customer.response;


import com.wallparisoft.ebill.customer.dto.ClientDto;
import lombok.Builder;
import lombok.experimental.SuperBuilder;


@SuperBuilder
public class ClientDtoResponse extends  BasicResponse{
    private ClientDto clientDto;
}
