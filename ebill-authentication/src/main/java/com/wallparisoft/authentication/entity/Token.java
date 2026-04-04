package com.wallparisoft.authentication.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@Table(name = "token", schema = "public")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Token extends BaseEntity {

  @Id
  @SequenceGenerator(sequenceName = "token_seq", name = "token_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq")
  private Long idToken;

  @Column(nullable = false, unique = true)
  private String token;

  @ManyToOne
  @JoinColumn(name = "id_user", referencedColumnName = "idUser")
  private User user;

  @Column(name = "expiration_date", nullable = false)
  private LocalDateTime expirationDate;

  public void setExpirationDate(int minutes) {
    LocalDateTime hoy = LocalDateTime.now();
    LocalDateTime exp = hoy.plusMinutes(minutes);
    this.expirationDate = exp;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isBefore(this.expirationDate);
  }
}
