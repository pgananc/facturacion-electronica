package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.repository.IClientRepo;
import com.wallparisoft.ebill.customer.service.IClientService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientServiceImpl implements IClientService {

	@Autowired
	private IClientRepo repo;


	@Override
	public Client save(Client entity) {
		return null;
	}

	@Override
	public Client update(Client entity, Long id) {
		return null;
	}

	@Override
	public List<Client> findAll() {
		return null;
	}

	@Override
	public Client findById(Long id) {
		return null;
	}
}
