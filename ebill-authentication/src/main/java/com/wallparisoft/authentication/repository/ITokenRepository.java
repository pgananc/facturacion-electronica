package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.Token;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ITokenRepository extends JpaRepository<Token, Long> {
  Token findByToken(String token);

  @Query("SELECT t.user.idUser FROM Token t WHERE t.token = :token")
  Long findUserIdByToken(@Param("token") String token);

  @Transactional
  @Modifying
  @Query("DELETE FROM Token t WHERE t.token = :token")
  void deleteByToken(@Param("token") String token);
}
