package com.wallparisoft.ebill.customer.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = "digital_signature")
public class DigitalSignature extends BaseEntity {
  @Id
  @SequenceGenerator(
      sequenceName = "digital_signature_seq",
      name = "digital_signature_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "digital_signature_seq")
  @Column(name = "id_digital_signature", nullable = false)
  private Long id;

  @Column(name = "sign_file")
  private byte[] signFile;

  @Column(name = "password")
  private String password;

  @Column(name = "effective_date")
  private Instant effectiveDate;

  @Column(name = "name_file")
  private String fileName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_company", referencedColumnName = "id_company")
  private Company company;
}
