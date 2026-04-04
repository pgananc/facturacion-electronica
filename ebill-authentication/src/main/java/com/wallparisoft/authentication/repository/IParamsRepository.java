package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.Params;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IParamsRepository extends JpaRepository<Params, Long> {
  Params findParamsByCodeParam(String codeParam);
}
