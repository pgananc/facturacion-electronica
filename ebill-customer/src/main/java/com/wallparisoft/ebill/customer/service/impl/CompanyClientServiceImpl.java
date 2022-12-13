package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import com.wallparisoft.ebill.customer.mapper.CompanyClientMapper;
import com.wallparisoft.ebill.customer.repository.IClientRepo;
import com.wallparisoft.ebill.customer.repository.ICompanyClientRepo;
import com.wallparisoft.ebill.customer.repository.ICompanyRepo;
import com.wallparisoft.ebill.customer.service.IClientService;
import com.wallparisoft.ebill.customer.service.ICompanyClientService;
import com.wallparisoft.ebill.customer.service.ICompanyService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyClientServiceImpl implements ICompanyClientService {

    @Autowired
    private ICompanyClientRepo companyClientRepo;
    @Autowired
    private ICompanyService companyService;
    @Autowired
    private IClientService clientService;
    @Autowired
    CompanyClientMapper companyClientMapper;

    @Autowired
    private ICompanyRepo companyRepo;

    @Override
    public CompanyClient save(CompanyClient entity) {
        return companyClientRepo.save(entity);
    }

    @Override
    public void delete(String companyIdentification, Long idClient) {
        CompanyClient companyClient = companyClientRepo.findByCompanyIdentificationAndClient_IdClient(companyIdentification, idClient)
                .orElseThrow(() -> new ModelNotFoundException("Company Client not found for this id company :: " + companyIdentification + ", " +
                        "and id client :: " + idClient));
        companyClientRepo.delete(companyClient);
    }

    @Override
    public CompanyClient saveCompanyClient(String companyIdentification, Long idClient) {
        Company company = companyRepo.findByIdentification(companyIdentification);
        Client client = clientService.findById(idClient);
        try {
            findByCompanyIdentificationAndIdClient(companyIdentification, idClient);
            return null;
        } catch (ModelNotFoundException me) {
            CompanyClient companyClient = CompanyClient.builder()
                    .companyIdentification(company.getIdentification())
                    .client(client)
                    .build();
            return save(companyClient);
        }
    }

    @Override
    public Page<ClientDto> getClientsFromACompanyPageable(String companyIdentification, Pageable pageable) {
        Page<CompanyClient> companyClients = companyClientRepo.findByCompanyIdentification(companyIdentification, pageable);
        if (companyClients == null) {
            throw new ModelNotFoundException("Company Client not found for this company id :: " + companyIdentification);
        }
        CompanyClientDto companyClientDtos = companyClientMapper.getClientsFromCompany(companyClients.getContent());

        return new PageImpl<>(companyClientDtos.getClients(), pageable, companyClientDtos.getClients().size());
    }

    @Override
    public CompanyClientDto getClientsFromACompany(String companyIdentification) {
        Optional<List<CompanyClient>> companyClientsOpt = companyClientRepo.findByCompanyIdentification(companyIdentification);
        if (companyClientsOpt.isPresent()) {
            return companyClientMapper.getClientsFromCompany(companyClientsOpt.get());
        }
        throw new ModelNotFoundException("Company Client not found for this company id :: " + companyIdentification);
    }

    @Override
    public CompanyClient update(CompanyClient entity, Long id) {
        //TODO add update implementation next USER STORIES
        return companyClientRepo.save(entity);
    }

    @Override
    public List<CompanyClient> findAll() {
        return companyClientRepo.findAll();
    }

    @Override
    public CompanyClient findById(Long id) {
        return this.companyClientRepo.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("Company Client not found for this id :: " + id));
    }

    @Override
    public CompanyClientDto findByCompanyIdentificationAndIdClient(String companyIdentification, Long idClient) {
        CompanyClient companyClient = companyClientRepo.findByCompanyIdentificationAndClient_IdClient(companyIdentification, idClient)
                .orElseThrow(() -> new ModelNotFoundException("Company Client not found for this id company :: " + companyIdentification + ", " +
                        "and id client :: " + idClient));
        return companyClientMapper.convertCompanyClientToCompanyClientDto(companyClient);
    }
}
