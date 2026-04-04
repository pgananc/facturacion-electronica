package com.wallparisoft.ebill.product.repository;

import com.wallparisoft.ebill.product.entity.TaxType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ITaxTypeRepo extends JpaRepository<TaxType, Long> {

  Optional<List<TaxType>> findByStatusOrderByName(@Param("status") boolean status);

  Optional<List<TaxType>> findByStatusAndTypeOrderByName(
      @Param("status") boolean status, @Param("type") String type);
}
