package com.wallparisoft.ebill.product.service;

import com.wallparisoft.ebill.product.entity.TaxType;
import com.wallparisoft.ebill.product.response.TaxTypeResponse;

public interface ITaxTypeService extends ICRUD<TaxType> {

  TaxTypeResponse getTaxTypesByStatus(boolean status);

  TaxTypeResponse getTaxTypesByStatusAndType(boolean status, String type);
}
