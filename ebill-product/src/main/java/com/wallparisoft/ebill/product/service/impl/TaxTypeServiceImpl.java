package com.wallparisoft.ebill.product.service.impl;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.entity.TaxType;
import com.wallparisoft.ebill.product.exception.ModelNotFoundException;
import com.wallparisoft.ebill.product.mapper.TaxTypeMapper;
import com.wallparisoft.ebill.product.repository.ITaxTypeRepo;
import com.wallparisoft.ebill.product.response.TaxTypeResponse;
import com.wallparisoft.ebill.product.service.ITaxTypeService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class TaxTypeServiceImpl implements ITaxTypeService {

  final ITaxTypeRepo repo;
  final TaxTypeMapper taxTypeMapper;

  @Override
  public TaxType save(TaxType obj) {
    return repo.save(obj);
  }

  @Override
  public TaxType update(TaxType obj, Long id) {
    TaxType taxType = findById(id);
    taxType.setType(obj.getType());
    taxType.setName(obj.getName());
    taxType.setValue(obj.getValue());
    taxType.setStatus(obj.getStatus());
    return repo.save(taxType);
  }

  @Override
  public List<TaxType> findAll() {
    return repo.findAll();
  }

  @Override
  public TaxType findById(Long id) {
    return this.repo
        .findById(id)
        .orElseThrow(() -> new ModelNotFoundException("TaxType not found for id: " + id));
  }

  @Override
  public TaxTypeResponse getTaxTypesByStatus(boolean status) {
    Optional<List<TaxType>> taxTypesOptional = repo.findByStatusOrderByName(status);
    TaxTypeResponse response =
        taxTypesOptional.isPresent()
            ? taxTypeMapper.toTaxTypeResponse(taxTypesOptional.get())
            : TaxTypeResponse.builder().build();
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public TaxTypeResponse getTaxTypesByStatusAndType(boolean status, String type) {
    Optional<List<TaxType>> taxTypesOptional = repo.findByStatusAndTypeOrderByName(status, type);
    TaxTypeResponse response =
        taxTypesOptional.isPresent()
            ? taxTypeMapper.toTaxTypeResponse(taxTypesOptional.get())
            : TaxTypeResponse.builder().build();
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }
}
