package com.wallparisoft.ebill.product.service;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.response.ProductResponse;

public interface IProductService extends ICRUD<Product> {

  ProductResponse getAllProducts();

  ProductResponse getProductsByStatus(boolean status);

  ProductResponse createProduct(ProductDto productDto);

  ProductResponse delete(Long id);

  ProductResponse updateProduct(ProductDto productDto, Long idProduct);
}
