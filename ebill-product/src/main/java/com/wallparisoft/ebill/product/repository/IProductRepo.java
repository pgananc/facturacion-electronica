package com.wallparisoft.ebill.product.repository;

import com.wallparisoft.ebill.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IProductRepo extends JpaRepository<Product, Long> {

    @Query(value = "SELECT c FROM Product  c WHERE c.status=:status order by c.mainCode")
    Optional<List<Product>> findProductsByStatus(@Param("status") boolean status);

    boolean existsByMainCode(String mainCode);

    @Query(value = "SELECT c FROM Product c where " +
            "c.mainCode like :mainCode and UPPER(c.name) like :name and " +
            "(c.productType =:productType or :productType=0 ) and c.status= :status " +
            "order by c.idProduct asc")
    Page<Product> findProductByMainCodeOrNameOrType(String mainCode, String name
            , Integer productType, Boolean status, Pageable pageable);
}
