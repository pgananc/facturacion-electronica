package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.Params;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IParamsRepo extends JpaRepository<Params, Long> {

  Optional<Params> findByCodeParam(String codeParam);

  Optional<Params> findByCodeParamAndStatusTrue(String codeParam);
}
