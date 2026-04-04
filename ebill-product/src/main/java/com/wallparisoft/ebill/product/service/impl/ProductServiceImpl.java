package com.wallparisoft.ebill.product.service.impl;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.exception.ModelNotFoundException;
import com.wallparisoft.ebill.product.mapper.ProductMapper;
import com.wallparisoft.ebill.product.repository.IProductRepo;
import com.wallparisoft.ebill.product.response.BasicResponse;
import com.wallparisoft.ebill.product.response.ProductResponse;
import com.wallparisoft.ebill.product.service.IProductService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

  final IProductRepo repo;
  final ProductMapper productMapper;

  @Override
  public Product save(Product obj) {
    return repo.save(obj);
  }

  public ProductResponse createProduct(ProductDto productDto) {
    ProductResponse response =
        productMapper.toProductResponse(save(productMapper.toProduct(productDto)));
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public ProductResponse delete(Long id) {
    Product product = findById(id);
    product.setStatus(false);
    ProductResponse response = productMapper.toProductResponse(repo.save(product));
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public BasicResponse deletePhysical(Long id) {
    findById(id);
    repo.deleteById(id);
    return BasicResponse.builder()
        .code(HttpStatus.OK.value())
        .status(HttpStatus.OK.name())
        .message("Product deleted permanently")
        .build();
  }

  @Override
  public ProductResponse updateProduct(ProductDto productDto, Long idProduct) {
    Product product = findById(idProduct);
    product.setName(productDto.getName());
    product.setDescription(productDto.getDescription());
    product.setMainCode(productDto.getMainCode());
    product.setUnitPrice(productDto.getUnitPrice());
    product.setStatus(productDto.getStatus());
    product.setProductType(productDto.getProductType());
    product.setIce(productDto.getIce());
    product.setTourismIva(productDto.getTourismIva());
    product.setIvaTaxType(productMapper.toProduct(productDto).getIvaTaxType());
    product.setIceTaxType(productMapper.toProduct(productDto).getIceTaxType());
    return productMapper.toProductResponse(save(product));
  }

  @Override
  public Page<ProductDto> findProductByIdCompanyAndMainCodeOrNameOrType(
      Long idCompany,
      String mainCode,
      String name,
      Integer productType,
      Boolean status,
      Pageable pageable) {

    mainCode = StringUtils.isBlank(mainCode) ? null : "%" + mainCode.trim() + "%";

    name = StringUtils.isBlank(name) ? null : "%" + name.trim().toUpperCase() + "%";

    if (productType != null && productType == 0) {
      productType = null;
    }

    Page<Product> products =
        repo.findProductByIdCompanyAndMainCodeOrNameOrType(
            idCompany, mainCode, name, productType, status, pageable);
    List<ProductDto> productDtoList = new ArrayList<>();
    products.getContent().parallelStream()
        .forEach(
            product -> {
              ProductDto productDto = productMapper.toProductDto(product);
              productDtoList.add(productDto);
            });
    return new PageImpl<>(productDtoList, pageable, products.getTotalElements());
  }

  @Override
  public boolean existsByMainCodeAndIdCompany(String mainCode, Long idCompany) {
    return repo.existsByMainCodeAndIdCompany(mainCode, idCompany);
  }

  @Override
  public ProductDto getProductById(Long idProduct) {
    return productMapper.toProductDto(findById(idProduct));
  }

  @Override
  public Product update(Product obj, Long id) {
    Product product = findById(id);

    return null;
  }

  @Override
  public List<Product> findAll() {
    return repo.findAll();
  }

  @Override
  public ProductResponse getAllProducts() {
    return productMapper.toProductResponse(findAll());
  }

  @Override
  public ProductResponse getProductsByStatus(boolean status) {
    Optional<List<Product>> products = repo.findProductsByStatusOrderByMainCode(status);
    ProductResponse response =
        products.isPresent()
            ? productMapper.toProductResponse(products.get())
            : ProductResponse.builder().build();
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public Product findById(Long id) {
    // TODO Auto-generated method stub
    return this.repo
        .findById(id)
        .orElseThrow(() -> new ModelNotFoundException("Product not found for id: " + id));
  }

  @Override
  public ProductResponse findProductsByIdCompanyAndStatusOrderByMainCode(Long idCompany) {
    Optional<List<Product>> productOptional =
        repo.findProductsByIdCompanyAndStatusOrderByMainCode(idCompany, true);
    ProductResponse response =
        productOptional.isPresent()
            ? productMapper.toProductResponse(productOptional.get())
            : ProductResponse.builder().build();
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public ProductResponse searchProductsForBilling(Long idCompany, String term) {
    List<Product> products = repo.searchProductsForBilling(idCompany, term);
    ProductResponse response = productMapper.toProductResponse(products);
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }
}
