package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAuthorizationRepo extends JpaRepository<AuthorizationSri, Long> {}
