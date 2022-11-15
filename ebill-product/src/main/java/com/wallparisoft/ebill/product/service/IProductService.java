package com.wallparisoft.ebill.product.service;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProductService extends ICRUD<Product> {

  ProductResponse getAllProducts();

  ProductResponse getProductsByStatus(boolean status);

  ProductResponse createProduct(ProductDto productDto);

  ProductResponse delete(Long id);

  ProductResponse updateProduct(ProductDto productDto, Long idProduct);

  Page<ProductDto> findProductByMainCodeOrNameOrType(String mainCode, String name, Integer productType, Boolean status, Pageable pageable);

  boolean existsByMainCode(String mainCode);

  ProductDto getProductById(Long idProduct);
}
