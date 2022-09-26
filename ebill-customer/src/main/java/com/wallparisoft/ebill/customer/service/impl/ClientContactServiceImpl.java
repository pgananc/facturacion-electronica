package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.entity.ClientContact;
import com.wallparisoft.ebill.customer.repository.IClientContactRepo;
import com.wallparisoft.ebill.customer.service.IClientContactService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientContactServiceImpl implements IClientContactService {

	@Autowired
	private IClientContactRepo clientContactRepo;


	@Override
	public ClientContact save(ClientContact entity) {
		return clientContactRepo.save(entity);
	}

	@Override
	public ClientContact update(ClientContact entity, Long id) {
		ClientContact contact = findById(id);
		return clientContactRepo.save(entity);
	}

	@Override
	public List<ClientContact> findAll() {
		return clientContactRepo.findAll();
	}

	@Override
	public ClientContact findById(Long id) {
		return this.clientContactRepo.findById(id)
				.orElseThrow(() -> new ModelNotFoundException("Client Contact not found for this id :: " + id));
	}
}
