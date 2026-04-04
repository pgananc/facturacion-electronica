package com.wallparisoft.ebill.commons.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public abstract class BaseEntity implements Serializable {

  @Column(name = "status", nullable = false)
  private Boolean status;

  @CreatedDate
  @Column(name = "creation_date", updatable = false)
  private LocalDateTime creationDate;

  @LastModifiedDate
  @Column(name = "update_date")
  private LocalDateTime updateDate;

  @PrePersist
  void preInsert() {
    creationDate = LocalDateTime.now();
  }

  @PreUpdate
  public void setUpdateDate() {
    updateDate = LocalDateTime.now();
  }
}
