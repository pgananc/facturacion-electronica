package com.wallparisoft.repository;

import com.wallparisoft.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITokenRepository extends JpaRepository<Token, Long> {
    Token findByToken(String token);
}
