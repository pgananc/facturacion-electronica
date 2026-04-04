package com.wallparisoft.ebillbilling.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "authorization_sri")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthorizationSri extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "authorization_sri_seq",
      sequenceName = "authorization_sri_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authorization_sri_seq")
  @Column(name = "id_authorization_sri")
  private Long idAuthorizationSri;

  @Column(name = "access_key")
  private String accessKey;

  @Column(name = "authorization_number")
  private String authorizationNumber;

  @Column(name = "authorization_date")
  private LocalDateTime authorizationDate;

  @Column(name = "authorization_status_sri")
  private String authorizationStatusSri;

  @Column(name = "reception_status_sri")
  private String receptionStatusSri;

  @Column(name = "reception_error", columnDefinition = "text")
  private String receptionError;

  @Column(name = "authorization_error", columnDefinition = "text")
  private String authorizationError;

  @Column(name = "reception_xml", columnDefinition = "text")
  private String receptionXml;

  @Column(name = "authorization_xml", columnDefinition = "text")
  private String authorizationXml;

  @Column(name = "authorization_storage_path")
  private String authorizationStoragePath;
}
