package com.wallparisoft.ebill.customer.repository;

import com.wallparisoft.ebill.customer.entity.DigitalSignature;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDigitalSignatureRepo extends JpaRepository<DigitalSignature, Long> {

  Optional<DigitalSignature> findByCompany_idCompanyAndStatus(Long idCompany, Boolean status);
}
