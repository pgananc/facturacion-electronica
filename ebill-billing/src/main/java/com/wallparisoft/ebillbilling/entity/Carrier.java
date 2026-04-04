package com.wallparisoft.ebillbilling.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "carrier")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Carrier extends BaseEntity {

  @Id
  @SequenceGenerator(name = "carrier_seq", sequenceName = "carrier_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "carrier_seq")
  @Column(name = "id_carrier")
  private Long idCarrier;

  @Column(name = "id_company", nullable = false)
  private Long idCompany;

  @Column(name = "identification_type", nullable = false, length = 2)
  private String identificationType;

  @Column(name = "identification", nullable = false, length = 13)
  private String identification;

  @Column(name = "name", nullable = false, length = 300)
  private String name;

  @Column(name = "address", length = 300)
  private String address;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "email", length = 100)
  private String email;

  @Column(name = "license_number", length = 50)
  private String licenseNumber;

  @Column(name = "vehicle_type", length = 50)
  private String vehicleType;
}
