package com.wallparisoft.ebill.customer.mapper;


import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyClientDto;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import org.mapstruct.Mapper;
import org.mapstruct.control.DeepClone;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", mappingControl = DeepClone.class)
public abstract class CompanyClientMapper {

    public CompanyClientDto convertCompanyClientToCompanyClientDto(CompanyClient companyClient) {
        return CompanyClientDto
                .builder()
                .idCompany(companyClient.getCompany().getIdCompany())
                .clients(List.of(ClientDto.builder()
                        .idClient(companyClient.getClient().getIdClient())
                        .clientType(companyClient.getClient().getClientType())
                        .identification(companyClient.getClient().getIdentification())
                        .name(companyClient.getClient().getName())
                        .idType(companyClient.getClient().getIdType())
                        .build()))
                .build();
    }

    public CompanyClientDto getClientsFromCompany(List<CompanyClient> companyClients) {
        CompanyClientDto companyClientDto = CompanyClientDto.builder()
                .idCompany(companyClients.get(0).getCompany().getIdCompany())
                .clients(new ArrayList<>()).build();
        for (CompanyClient companyClient : companyClients) {
            companyClientDto.getClients()
                    .add(ClientDto
                            .builder()
                            .idClient(companyClient.getClient().getIdClient())
                            .clientType(companyClient.getClient().getClientType())
                            .identification(companyClient.getClient().getIdentification())
                            .name(companyClient.getClient().getName())
                            .idType(companyClient.getClient().getIdType())
                            .build());
        }
        return companyClientDto;
    }
}
