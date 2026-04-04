package com.wallparisoft.authentication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "feature_permission", schema = "public")
public class FeaturePermission {

  @Id
  @SequenceGenerator(
      sequenceName = "feature_permission_seq",
      name = "feature_permission_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feature_permission_seq")
  @Column(name = "id_permission")
  private Long idPermission;

  @Column(name = "feature_code", nullable = false, unique = true, length = 50)
  private String featureCode;

  @Column(name = "feature_name", nullable = false, length = 100)
  private String featureName;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "module", length = 50)
  private String module;

  @Column(name = "allowed_roles", columnDefinition = "integer[]")
  private Integer[] allowedRoles;

  @Column(name = "required_plans", columnDefinition = "varchar(50)[]")
  private String[] requiredPlans;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
