package com.wallparisoft.ebill.product.service.impl;

import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.repository.IProductRepo;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wallparisoft.ebill.product.service.IProductService;

@Service
public class ProductServiceImpl implements IProductService {

	@Autowired
	private IProductRepo repo;

	@Override
	public Product save(Product obj) {
		return repo.save(obj);
	}

	@Override
	public Product update(Product obj, Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product> findAll() {
		return repo.findAll();
	}

	@Override
	public Product findById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}


}
