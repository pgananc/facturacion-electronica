package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.exception.ModelNotFoundException;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.customer.service.IContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactServiceImpl implements IContactService {

	@Autowired
	private IContactRepo contactRepo;


	@Override
	public Contact save(Contact entity) {
		return contactRepo.save(entity);
	}

	@Override
	public Contact update(Contact entity, Long id) {
		Contact contact = findById(id);
		return contactRepo.save(entity);
	}

	@Override
	public List<Contact> findAll() {
		return contactRepo.findAll();
	}

	@Override
	public Contact findById(Long id) {
		return this.contactRepo.findById(id)
				.orElseThrow(() -> new ModelNotFoundException("Contact not found for this id :: " + id));
	}
}
