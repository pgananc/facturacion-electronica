package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.entity.CompanyContact;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.mapper.CompanyMapper;
import com.wallparisoft.ebill.customer.mapper.ContactMapper;
import com.wallparisoft.ebill.customer.repository.ICompanyContactRepo;
import com.wallparisoft.ebill.customer.repository.ICompanyRepo;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.customer.response.CompanyDtoResponse;
import com.wallparisoft.ebill.customer.service.ICompanyService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyServiceImpl implements ICompanyService {

    @Autowired
    ICompanyRepo companyRepo;
    @Autowired
    IContactRepo contactRepo;
    @Autowired
    ICompanyContactRepo companyContactRepo;
    @Autowired
    CompanyMapper companyMapper;
    @Autowired
    ContactMapper contactMapper;

    @Override
    public Company save(Company entity) {
        return companyRepo.save(entity);
    }

    @Override
    public Company update(Company entity, Long id) {
        return null;
    }

    @Override
    public List<Company> findAll() {
        return companyRepo.findAll();
    }

    @Override
    public Company findById(Long id) {
        return this.companyRepo.findById(id).orElseThrow(() -> new ModelNotFoundException("Company not found for this id :: " + id));

    }

    @Override
    @Transactional
    public void saveCompanyAndContact(CompanyDto companyDto) {
        Company company = companyMapper.convertCompanyDtoToCompany(companyDto);
        List<Contact> contacts = contactMapper.convertContactDtoListToContactList(companyDto.getContacts());
        Company companySave = save(company);
        contacts.forEach(x -> {
            Contact contact = contactRepo.save(x);
            CompanyContact companyContact = CompanyContact.builder()
                    .company(companySave)
                    .contact(contact)
                    .status(contact.getStatus()).build();
            companyContactRepo.save(companyContact);

        });
    }

    @Override
    @Transactional
    public void updateCompanyAndContact(CompanyDto companyDto, Long idCompany) {
        Company companySave = findById(idCompany);
        Company company = companyMapper.convertCompanyDtoToCompany(companyDto);
        List<Contact> contacts = contactMapper.convertContactDtoListToContactList(companyDto.getContacts());

        company.setCreationDate(companySave.getCreationDate());
        save(company);
        contacts.forEach(contact -> {
            Optional<CompanyContact> companyContactOptional = companyContactRepo.findByIdCompanyAndIdContact(company.getIdCompany(), contact.getIdContact());
            if (companyContactOptional.isPresent()) {
                CompanyContact companyContact = companyContactOptional.get();
                companyContact.setStatus(contact.getStatus());
                companyContactRepo.save(companyContact);
                contact.setCreationDate(companyContact.getCreationDate());
                contactRepo.save(contact);
            }
        });
    }

    @Override
    @Transactional
    public void updateCompany(CompanyDto companyDto) {
        Company companySave = findById(companyDto.getIdCompany());
        if (companySave == null) {
            throw new ModelNotFoundException("No existe id de compañía consultado: " + companyDto.getIdCompany());
        }
        Company company = companyMapper.convertCompanyDtoToCompany(companyDto);
        company.setCreationDate(companySave.getCreationDate());
        company.setUpdateDate(LocalDateTime.now());
        save(company);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Company company = findById(id);
        company.setStatus(false);
        companyRepo.save(company);
    }

    @Override
    public List<CompanyDto> findCompaniesActiveAndContactActive() {
        List<CompanyDto> companyList = new ArrayList<>();
         List<Company> companies = this.companyRepo.findByStatusIsTrueOrderByIdentification().orElse(new ArrayList<>());
        companies.parallelStream().forEach(x -> {
            CompanyDto companyDto = companyMapper.convertCompanyToCompanyDto(x);
            List<Contact> contacts = contactRepo.findCompanyContactActiveByIdCompany(x.getIdCompany());
            List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
            companyDto.setContacts(contactsDto);
            companyList.add(companyDto);
        });
        return companyList;
    }

    @Override
    public Page<CompanyDto> findCompanyByIdentificationOrNameOrBranchOfficeCode(String identification, String name, String branchOfficeCode, Boolean status, Pageable pageable) {
        Page<Company> companies = companyRepo.findCompanyByIdentificationOrNameOrBranchOfficeCode("%" + identification + "%",
                "%" + name.toUpperCase() + "%", "%" + branchOfficeCode.toUpperCase() + "%", status, pageable);
        List<CompanyDto> companyDtoList = new ArrayList<>();
        companies.getContent().parallelStream().forEach(company -> {
            CompanyDto companyDto = companyMapper.convertCompanyToCompanyDto(company);
            companyDtoList.add(companyDto);
        });
        return new PageImpl<>(companyDtoList, pageable, companies.getTotalElements());
    }

    @Override
    public boolean existsByIdentificationAndBranchOfficeCode(String identification, String branchOfficeCode) {
        return companyRepo.existsByIdentificationAndBranchOfficeCode(identification, branchOfficeCode);
    }

    @Override
    @Transactional
    public void saveCompany(CompanyDto companyDto) {
        companyRepo.save(companyMapper.convertCompanyDtoToCompany(companyDto));
    }

    @Override
    public CompanyDto findCompanyById(Long id) {
        return companyMapper.convertCompanyToCompanyDto(findById(id));
    }

    @Override
    public CompanyDtoResponse getAllCompanies() {
        return CompanyDtoResponse.builder().companyDtos(companyMapper.convertCompaniesToCompaniesDto(companyRepo.findAll())).build();
    }

    @Override
    public CompanyDtoResponse getCompaniesByIdCompanies(List<Long> idCompanies) {
        return CompanyDtoResponse
                .builder()
                .companyDtos(companyMapper.convertCompaniesToCompaniesDto(companyRepo.findByIdCompanyInAndStatus(idCompanies,true)))
                .build();
    }
}