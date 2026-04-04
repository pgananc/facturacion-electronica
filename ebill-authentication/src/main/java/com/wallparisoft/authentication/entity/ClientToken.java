package com.wallparisoft.authentication.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_token", schema = "public")
public class ClientToken extends BaseEntity {

  @Id
  @Column(name = "id_client_token")
  @SequenceGenerator(
      sequenceName = "client_token_seq",
      name = "client_token_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_token_seq")
  private Long idToken;

  @Column(name = "client_number", nullable = false, length = 20)
  private String clientNumber;

  @Column(name = "client_email", nullable = false, length = 255)
  private String clientEmail;

  @Column(name = "token", nullable = false, length = 100, unique = true)
  private String token;

  @Column(name = "expiry_date", nullable = false)
  private LocalDateTime expiryDate;

  @Column(name = "used_date")
  private LocalDateTime usedDate;

  /** Sets the expiry date to 5 minutes from now */
  public void setExpirationDate(int minutes) {
    LocalDateTime hoy = LocalDateTime.now();
    LocalDateTime exp = hoy.plusMinutes(minutes);
    this.expiryDate = exp;
  }

  /**
   * Checks if the token is expired
   *
   * @return true if token is expired, false otherwise
   */
  public boolean isExpired() {
    return LocalDateTime.now().isBefore(this.expiryDate);
  }

  /** Marks the token as used */
  public void markAsUsed() {
    setStatus(Boolean.FALSE);
    this.usedDate = LocalDateTime.now();
  }
}
