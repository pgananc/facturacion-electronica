package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.entity.ClientContact;
import com.wallparisoft.ebill.customer.repository.IClientContactRepo;
import com.wallparisoft.ebill.customer.service.IClientContactService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClientContactServiceImpl implements IClientContactService {

  private final IClientContactRepo clientContactRepo;

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
    return this.clientContactRepo
        .findById(id)
        .orElseThrow(
            () -> new ModelNotFoundException("Client Contact not found for this id :: " + id));
  }
}
