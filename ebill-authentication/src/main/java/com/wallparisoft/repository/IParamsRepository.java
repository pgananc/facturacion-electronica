package com.wallparisoft.repository;

import com.wallparisoft.entity.Params;
import com.wallparisoft.entity.ResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IParamsRepository extends JpaRepository<Params, Long> {
Params findParamsByCodeParam(String codeParam);
}
