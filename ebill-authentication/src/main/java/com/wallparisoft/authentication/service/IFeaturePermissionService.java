package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.dto.BulkUpdateRolesDto;
import com.wallparisoft.authentication.dto.FeaturePermissionDto;
import com.wallparisoft.authentication.dto.FeatureValidationRequestDto;
import com.wallparisoft.authentication.dto.FeatureValidationResponseDto;
import com.wallparisoft.authentication.dto.RolePermissionsDto;
import com.wallparisoft.authentication.dto.UpdateFeaturePlansDto;
import com.wallparisoft.authentication.dto.UpdateFeatureRolesDto;
import java.util.List;

public interface IFeaturePermissionService {

  /**
   * Validate complete feature access (Role + Plan)
   * This is the coordinator that performs both validations:
   * 1. Check role permission (local)
   * 2. Call ebill-billing to check plan subscription
   * 3. Verify plan includes the feature
   *
   * @param request Contains featureCode, idCompany and idUser
   * @return Response indicating if access is allowed and reason if denied
   */
  FeatureValidationResponseDto validateFeatureAccess(FeatureValidationRequestDto request);

  // ============================================
  // ROLE PERMISSION MANAGEMENT (New endpoints)
  // ============================================

  /**
   * Get all features with their details (for management)
   * Returns FeaturePermissionDto instead of entity for better control
   */
  List<FeaturePermissionDto> getAllFeaturesForManagement();

  /**
   * Get all permissions for a specific role
   *
   * @param roleId The role ID to query
   * @return RolePermissionsDto with all features accessible by this role
   */
  RolePermissionsDto getRolePermissions(Integer roleId);

  /**
   * Update allowed roles for a specific feature
   * Only super_admin can perform this operation
   *
   * @param featureCode The feature to update
   * @param updateDto Contains the new list of allowed roles
   * @return Updated feature permission DTO
   */
  FeaturePermissionDto updateFeatureRoles(String featureCode, UpdateFeatureRolesDto updateDto);

  /**
   * Bulk update allowed roles for multiple features
   *
   * @param bulkDto Contains map of featureCode -> allowedRoles
   * @return List of updated feature permission DTOs
   */
  List<FeaturePermissionDto> bulkUpdateRoles(BulkUpdateRolesDto bulkDto);

  // ============================================
  // PLAN PERMISSION MANAGEMENT (New endpoints)
  // ============================================

  /**
   * Get required plans for a specific feature
   *
   * @param featureCode The feature code
   * @return Feature permission DTO with plan information
   */
  FeaturePermissionDto getFeaturePlans(String featureCode);

  /**
   * Update required plans for a specific feature
   *
   * @param featureCode The feature to update
   * @param updateDto Contains the new list of required plans
   * @return Updated feature permission DTO
   */
  FeaturePermissionDto updateFeaturePlans(String featureCode, UpdateFeaturePlansDto updateDto);

  // ============================================
  // USER FEATURES ENDPOINT
  // ============================================

  /**
   * Get all features allowed for a user in a company
   * Validates both role and plan permissions
   *
   * @param idUser The user ID
   * @param idCompany The company ID
   * @return List of feature codes that the user can access
   */
  List<String> getUserAllowedFeatures(Long idUser, Long idCompany);
}
