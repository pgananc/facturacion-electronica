package com.wallparisoft.ebill.customer.response;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class ClientDtoResponse extends BasicResponse {
  private List<ClientDto> clientDtos;
}
