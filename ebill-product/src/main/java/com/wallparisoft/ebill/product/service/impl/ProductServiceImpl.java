package com.wallparisoft.ebill.product.service.impl;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.dto.ProductDto;
import com.wallparisoft.ebill.product.entity.Product;
import com.wallparisoft.ebill.product.exception.ModelNotFoundException;
import com.wallparisoft.ebill.product.mapper.ProductServiceMapper;
import com.wallparisoft.ebill.product.repository.IProductRepo;
import com.wallparisoft.ebill.product.response.ProductResponse;
import com.wallparisoft.ebill.product.service.IProductService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = PRIVATE)
@Log4j2
public class ProductServiceImpl implements IProductService {

    @Autowired
    IProductRepo repo;
    @Autowired
    ProductServiceMapper serviceMapper;


    @Override
    public Product save(Product obj) {
        return repo.save(obj);
    }

    public ProductResponse createProduct(ProductDto productDto) {
        ProductResponse response = serviceMapper.toProductResponse(
                save(serviceMapper.toProduct(productDto)));
        response.setCode(HttpStatus.OK.value());
        response.setStatus(HttpStatus.OK.name());
        return response;
    }

    @Override
    public ProductResponse delete(Long id) {
        Product product = findById(id);
        product.setStatus(false);
        ProductResponse response = serviceMapper
                .toProductResponse(repo.save(product));
        response.setCode(HttpStatus.OK.value());
        response.setStatus(HttpStatus.OK.name());
        return response;
    }

    @Override
    public ProductResponse updateProduct(ProductDto productDto, Long idProduct) {
        Product product = findById(idProduct);
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setDiscount(productDto.getDiscount());
        product.setAuxiliarCode(productDto.getAuxiliarCode());
        product.setMainCode(productDto.getMainCode());
        product.setUnitPrice(productDto.getUnitPrice());
        product.setStatus(productDto.isStatus());
        product.setProductType(product.getProductType());
        return serviceMapper.toProductResponse(save(product));
    }

    @Override
    public Page<ProductDto> findProductByMainCodeOrNameOrType(String mainCode, String name, Integer productType, Boolean status, Pageable pageable) {
        Page<Product> products = repo.findProductByMainCodeOrNameOrType("%" + mainCode + "%", "%" + name.toUpperCase() + "%", productType, status, pageable);
        List<ProductDto> productDtoList = new ArrayList<>();
        products.getContent().parallelStream().forEach(product -> {
            ProductDto productDto = serviceMapper.toProductDto(product);
            productDtoList.add(productDto);
        });
        return new PageImpl<>(productDtoList, pageable, products.getTotalElements());
    }

    @Override
    public boolean existsByMainCode(String mainCode) {
        return repo.existsByMainCode(mainCode);
    }

    @Override
    public ProductDto getProductById(Long idProduct) {
        return serviceMapper.toProductDto(findById(idProduct));
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
        return serviceMapper.toProductResponse(findAll());
    }

    @Override
    public ProductResponse getProductsByStatus(boolean status) {
        Optional<List<Product>> products = repo.findProductsByStatusOrderByMainCode(status);
        ProductResponse response = products.isPresent() ? serviceMapper
                .toProductResponse(products.get()) : ProductResponse.builder().build();
        response.setCode(HttpStatus.OK.value());
        response.setStatus(HttpStatus.OK.name());
        return response;
    }

    @Override
    public Product findById(Long id) {
        // TODO Auto-generated method stub
        return this.repo.findById(id).orElseThrow(() -> new ModelNotFoundException("Product not found for id: " + id));
    }

}