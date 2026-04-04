package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.Carrier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICarrierRepo extends JpaRepository<Carrier, Long> {

  List<Carrier> findByIdCompany(Long idCompany);

  Page<Carrier> findByIdCompany(Long idCompany, Pageable pageable);

  List<Carrier> findByIdCompanyAndStatusTrue(Long idCompany);

  Page<Carrier> findByIdCompanyAndStatusTrue(Long idCompany, Pageable pageable);

  Optional<Carrier> findByIdCompanyAndIdentification(Long idCompany, String identification);
}
