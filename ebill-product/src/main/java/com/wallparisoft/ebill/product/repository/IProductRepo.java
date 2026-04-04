package com.wallparisoft.ebill.product.repository;

import com.wallparisoft.ebill.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IProductRepo extends JpaRepository<Product, Long> {

  @Query(
      """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.ivaTaxType
        LEFT JOIN FETCH p.iceTaxType
        WHERE p.status = :status
        ORDER BY p.mainCode
      """)
  Optional<List<Product>> findProductsByStatusOrderByMainCode(@Param("status") boolean status);

  boolean existsByMainCodeAndIdCompany(String mainCode, Long idCompany);

  @Query(
      """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.ivaTaxType
        LEFT JOIN FETCH p.iceTaxType
        WHERE (:idCompany IS NULL OR p.idCompany = :idCompany)
          AND (:mainCode IS NULL OR p.mainCode LIKE :mainCode)
          AND (:name IS NULL OR UPPER(p.name) LIKE :name)
          AND (:productType IS NULL OR p.productType = :productType)
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.idProduct ASC
      """)
  Page<Product> findProductByIdCompanyAndMainCodeOrNameOrType(
      Long idCompany,
      String mainCode,
      String name,
      Integer productType,
      Boolean status,
      Pageable pageable);

  @Query(
      """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.ivaTaxType
        LEFT JOIN FETCH p.iceTaxType
        WHERE p.idCompany = :idCompany
          AND p.status = :status
        ORDER BY p.mainCode
      """)
  Optional<List<Product>> findProductsByIdCompanyAndStatusOrderByMainCode(
      @Param("idCompany") Long idCompany, @Param("status") boolean status);

  @Query(
      """
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.ivaTaxType
        LEFT JOIN FETCH p.iceTaxType
        WHERE p.idCompany = :idCompany
          AND p.status = true
          AND (:term IS NULL OR :term = ''
               OR UPPER(p.mainCode) LIKE UPPER(CONCAT('%', :term, '%'))
               OR UPPER(p.name) LIKE UPPER(CONCAT('%', :term, '%'))
               OR UPPER(p.description) LIKE UPPER(CONCAT('%', :term, '%')))
        ORDER BY p.mainCode ASC
      """)
  List<Product> searchProductsForBilling(
      @Param("idCompany") Long idCompany, @Param("term") String term);
}
