package com.wallparisoft.repository;

import com.wallparisoft.entity.ResetToken;
import com.wallparisoft.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IRestTokenRepository extends JpaRepository<ResetToken, Long> {
    ResetToken findByToken(String token);
}
