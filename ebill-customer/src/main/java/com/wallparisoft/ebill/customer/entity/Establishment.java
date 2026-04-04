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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = false)
@Table(name = "establishment")
public class Establishment extends BaseEntity {
  @Id
  @SequenceGenerator(
      sequenceName = "establishment_seq",
      name = "establishment_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "establishment_seq")
  @Column(name = "id_establishment", nullable = false)
  private Long idEstablishment;

  @Size(max = 100)
  @Column(name = "name", length = 100)
  private String name;

  @Size(max = 50)
  @Column(name = "code", length = 50)
  private String code;

  @Size(max = 255)
  @Column(name = "address")
  private String address;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_company")
  private Company company;

  @Size(max = 8)
  @Column(name = "type", length = 8)
  private String type;

  @OneToMany(mappedBy = "establishment")
  private Set<InvoiceIssuancePoint> invoiceIssuancePoints = new LinkedHashSet<>();
}
