package com.wallparisoft.ebill.customer.response;


import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;


@SuperBuilder
@Getter
public class ClientDtoResponse extends BasicResponse {
    private List<ClientDto> clientDtos;

}
