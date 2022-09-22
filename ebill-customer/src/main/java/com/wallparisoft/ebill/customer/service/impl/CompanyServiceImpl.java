package com.wallparisoft.ebill.customer.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.*;
import com.wallparisoft.ebill.customer.mapper.CompanyMapper;
import com.wallparisoft.ebill.customer.mapper.ContactMapper;
import com.wallparisoft.ebill.customer.repository.ICompanyContactRepo;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wallparisoft.ebill.customer.repository.ICompanyRepo;
import com.wallparisoft.ebill.customer.service.ICompanyService;

import javax.transaction.Transactional;

@Service
public class CompanyServiceImpl implements ICompanyService {

	@Autowired
	private ICompanyRepo companyRepo;
	@Autowired
	private IContactRepo contactRepo;
	@Autowired
	private ICompanyContactRepo companyContactRepo;
	@Autowired
	private CompanyMapper companyMapper;
	@Autowired
	private ContactMapper contactMapper;

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
					.status(contact.isStatus()).build();
			companyContactRepo.save(companyContact);

		});
	}

	@Override
	public void updateCompanyAndContact(CompanyDto companyDto, Long idCompany) {
		Company companySave = findById(idCompany);
		Company company = companyMapper.convertCompanyDtoToCompany(companyDto);
		List<Contact> contacts = contactMapper.convertContactDtoListToContactList(companyDto.getContacts());

		company.setCreationDate(companySave.getCreationDate());
		save(company);
		contacts.forEach(x -> {
			Optional<CompanyContact> companyContactOptional = companyContactRepo.findByIdCompanyAndIdContact(company.getIdCompany(), x.getIdContact());
			if (companyContactOptional.isPresent()) {
				CompanyContact companyContact = companyContactOptional.get();
				companyContact.setStatus(x.isStatus());
				companyContactRepo.save(companyContact);
				x.setCreationDate(companyContact.getCreationDate());
				contactRepo.save(x);
			}
		});
	}

	@Override
	public void delete(Long id) {
		Company company = findById(id);
		company.setStatus(false);
		companyRepo.save(company);
	}

	@Override
	public List<CompanyDto> findCompaniesActiveAndContactActive() {
		List<CompanyDto> companyList = new ArrayList<>();
		List<Company> companies = this.companyRepo.findCompaniesActive();
		companies.parallelStream().forEach(x -> {
			CompanyDto companyDto = companyMapper.convertCompanyToCompanyDto(x);
			List<Contact> contacts = contactRepo.findCompanyContactActiveByIdCompany(x.getIdCompany());
			List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
			companyDto.setContacts(contactsDto);
			companyList.add(companyDto);
		});
		return companyList;
	}
}
