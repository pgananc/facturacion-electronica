package com.wallparisoft.ebill.customer.service.impl;

import java.util.List;

import com.wallparisoft.ebill.customer.entity.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wallparisoft.ebill.customer.repository.ICompanyRepo;
import com.wallparisoft.ebill.customer.service.ICompanyService;

@Service
public class CompanyServiceImpl implements ICompanyService {

	@Autowired
	private ICompanyRepo repo;


	@Override
	public Company save(Company entity) {
		return null;
	}

	@Override
	public Company update(Company entity, Long id) {
		return null;
	}

	@Override
	public List<Company> findAll() {
		return null;
	}

	@Override
	public Company findById(Long id) {
		return null;
	}
}
