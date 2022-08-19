package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.service.IPurchaseProofService;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wallparisoft.ebillbilling.entity.PurchaseProof;
import com.wallparisoft.ebillbilling.repository.IPurchaseProofRepo;

@Service
public class PurchaseProofServiceImpl implements IPurchaseProofService {

	@Autowired
	private IPurchaseProofRepo repo;

	@Override
	public PurchaseProof register(PurchaseProof obj) {
		return repo.save(obj);
	}

	@Override
	public PurchaseProof modify(PurchaseProof obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PurchaseProof> list() {
		return repo.findAll();
	}

	@Override
	public PurchaseProof getById(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(Integer id) {
		// TODO Auto-generated method stub
		return false;
	}

}
