package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.FeaturePermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IFeaturePermissionRepository extends JpaRepository<FeaturePermission, Long> {

  /** Find a feature by its unique code */
  Optional<FeaturePermission> findByFeatureCode(String featureCode);

  /** Find a feature by code and active status */
  Optional<FeaturePermission> findByFeatureCodeAndIsActive(String featureCode, Boolean isActive);

  /** Find all active features */
  List<FeaturePermission> findByIsActiveTrue();

  /** Find all features by module */
  List<FeaturePermission> findByModuleAndIsActiveTrue(String module);

  /**
   * Find features that a specific role can access Uses PostgreSQL array operators to check if
   * roleId is in allowedRoles array
   */
  @Query(
      value =
          "SELECT * FROM feature_permission WHERE :roleId = ANY(allowed_roles) AND is_active = true",
      nativeQuery = true)
  List<FeaturePermission> findFeaturesByRole(@Param("roleId") Integer roleId);

  /** Check if a role has access to a specific feature */
  @Query(
      value =
          "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END "
              + "FROM feature_permission "
              + "WHERE feature_code = :featureCode "
              + "AND :roleId = ANY(allowed_roles) "
              + "AND is_active = true",
      nativeQuery = true)
  boolean hasRoleAccess(@Param("featureCode") String featureCode, @Param("roleId") Integer roleId);

  /** Find features that require a specific plan */
  @Query(
      value =
          "SELECT * FROM feature_permission WHERE :planCode = ANY(required_plans) AND is_active = true",
      nativeQuery = true)
  List<FeaturePermission> findFeaturesByPlan(@Param("planCode") String planCode);
}
