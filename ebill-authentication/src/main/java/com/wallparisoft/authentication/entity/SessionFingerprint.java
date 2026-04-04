package com.wallparisoft.authentication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "session_fingerprint",
    indexes = {
      @Index(name = "idx_session_fingerprint_user", columnList = "user_id"),
      @Index(name = "idx_session_fingerprint_session_id", columnList = "session_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionFingerprint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false, unique = true)
  private UUID sessionId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "ip_address", nullable = false, length = 45)
  private String ipAddress;

  @Column(name = "user_agent", nullable = false, length = 500)
  private String userAgent;

  @Column(name = "browser_base", nullable = false, length = 50)
  private String browserBase;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "last_activity_at", nullable = false)
  private LocalDateTime lastActivityAt;
}
