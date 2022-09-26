package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.entity.CompanyContact;
import com.wallparisoft.ebill.customer.repository.ICompanyContactRepo;
import com.wallparisoft.ebill.customer.service.ICompanyContactService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyContactServiceImpl implements ICompanyContactService {

	@Autowired
	private ICompanyContactRepo companyContactRepo;

	@Override
	public CompanyContact save(CompanyContact entity) {
		return companyContactRepo.save(entity);
	}

	@Override
	public CompanyContact update(CompanyContact entity, Long id) {
		CompanyContact contact = findById(id);
		return companyContactRepo.save(entity);
	}

	@Override
	public List<CompanyContact> findAll() {
		return companyContactRepo.findAll();
	}

	@Override
	public CompanyContact findById(Long id) {
		return this.companyContactRepo.findById(id)
				.orElseThrow(() -> new ModelNotFoundException("Contact Company not found for this id :: " + id));
	}
}
