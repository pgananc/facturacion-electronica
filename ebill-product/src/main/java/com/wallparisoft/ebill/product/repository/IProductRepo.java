package com.wallparisoft.ebill.product.repository;

import com.wallparisoft.ebill.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IProductRepo extends JpaRepository<Product, Long> {

  @Query(value = "SELECT c FROM Product  c WHERE c.status=:status order by c.mainCode")
  Optional<List<Product>> findProductsByStatus(@Param("status") boolean status);
}
