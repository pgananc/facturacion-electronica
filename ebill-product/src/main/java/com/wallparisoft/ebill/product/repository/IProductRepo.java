package com.wallparisoft.ebill.product.repository;

import com.wallparisoft.ebill.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IProductRepo extends JpaRepository<Product, Long> {

}
