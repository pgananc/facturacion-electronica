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

/**
 * Entidad que representa el catálogo de motivos de nota de crédito según SRI Ecuador.
 *
 * <p>Códigos válidos: - 01: Devolución de bienes - 02: Descuento comercial posterior - 03:
 * Corrección de error en facturación - 04: Rebaja o bonificación - 05: Resciliación del contrato
 */
@Entity
@Table(name = "credit_note_reason")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditNoteReason extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "credit_note_reason_seq",
      sequenceName = "credit_note_reason_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credit_note_reason_seq")
  @Column(name = "id_reason")
  Long idReason;

  @Column(name = "code", length = 2, nullable = false, unique = true)
  String code;

  @Column(name = "description", length = 100, nullable = false)
  String description;
}
