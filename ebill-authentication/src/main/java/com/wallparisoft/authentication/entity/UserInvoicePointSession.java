package com.wallparisoft.authentication.entity;

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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_invoice_point_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInvoicePointSession {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_invoice_point_session_seq")
  @SequenceGenerator(
      name = "user_invoice_point_session_seq",
      sequenceName = "user_invoice_point_session_seq",
      allocationSize = 1)
  @Column(name = "id_user_invoice_point_session")
  private Long idUserInvoicePointSession;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_user")
  private User user;

  @Column(name = "id_invoice_issuance_point", nullable = false)
  private Long idInvoiceIssuancePoint;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time")
  private LocalDateTime endTime;

  @Column(name = "last_activity")
  private LocalDateTime lastActivity;

  @Column(name = "active")
  private Boolean active;
}
