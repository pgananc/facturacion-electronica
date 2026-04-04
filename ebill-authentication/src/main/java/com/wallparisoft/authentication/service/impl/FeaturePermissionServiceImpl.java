package com.wallparisoft.authentication.service.impl;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_002;

import com.wallparisoft.authentication.client.IBillingClient;
import com.wallparisoft.authentication.dto.BulkUpdateRolesDto;
import com.wallparisoft.authentication.dto.FeaturePermissionDto;
import com.wallparisoft.authentication.dto.FeatureValidationRequestDto;
import com.wallparisoft.authentication.dto.FeatureValidationResponseDto;
import com.wallparisoft.authentication.dto.RolePermissionsDto;
import com.wallparisoft.authentication.dto.SubscriptionResponseDto;
import com.wallparisoft.authentication.dto.UpdateFeaturePlansDto;
import com.wallparisoft.authentication.dto.UpdateFeatureRolesDto;
import com.wallparisoft.authentication.entity.FeaturePermission;
import com.wallparisoft.authentication.entity.Menu;
import com.wallparisoft.authentication.entity.MenuRole;
import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.entity.UserCompanyRole;
import com.wallparisoft.authentication.repository.IMenuRepository;
import com.wallparisoft.authentication.repository.IMenuRoleRepository;
import com.wallparisoft.authentication.repository.IRoleRepository;
import com.wallparisoft.authentication.exception.ModelNotFoundException;
import com.wallparisoft.authentication.repository.IFeaturePermissionRepository;
import com.wallparisoft.authentication.repository.IUserCompanyRepo;
import com.wallparisoft.authentication.repository.IUserCompanyRoleRepository;
import com.wallparisoft.authentication.service.IFeaturePermissionService;
import com.wallparisoft.ebill.utils.log.EventLog;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeaturePermissionServiceImpl implements IFeaturePermissionService {

  private final IFeaturePermissionRepository featurePermissionRepository;
  private final IUserCompanyRepo userCompanyRepo;
  private final IUserCompanyRoleRepository userCompanyRoleRepository;
  private final IBillingClient billingClient;
  private final EntityManager entityManager;
  private final IMenuRepository menuRepository;
  private final IMenuRoleRepository menuRoleRepository;
  private final IRoleRepository roleRepository;

  private static final String SUPER_ADMIN_ROLE = "ROLE_ADMIN_GLOBAL";
  private static final Integer SUPER_ADMIN_ROLE_ID = 1;
  private static final String SUBSCRIPTION_STATUS_EXPIRED = "EXPIRED";
  private static final String FEATURE_CODE_PREFIX_INVOICE = "INVOICE_";
  private static final String URL_SUBSCRIPTIONS_RENEW = "/subscriptions/renew";
  private static final String URL_SUBSCRIPTIONS_PLANS = "/subscriptions/plans";
  private static final String URL_SUBSCRIPTIONS_UPGRADE = "/subscriptions/upgrade";


  @Override
  public FeatureValidationResponseDto validateFeatureAccess(FeatureValidationRequestDto request) {
    String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method(methodName)
            .information(
                String.format(
                    "Validating feature access for featureCode=%s, idCompany=%s, idUser=%s",
                    request.getFeatureCode(), request.getIdCompany(), request.getIdUser())),
        REQUEST,
        LEVEL_001);

    try {
      // 1. Get feature
      FeaturePermission feature =
          featurePermissionRepository
              .findByFeatureCodeAndIsActive(request.getFeatureCode(), true)
              .orElseThrow(
                  () ->
                      new ModelNotFoundException(
                          "Feature not found or inactive: " + request.getFeatureCode()));

      // 2. Get specific user from company
      UserCompany userCompany = userCompanyRepo
          .findByIdCompanyAndUser_IdUser(request.getIdCompany(), request.getIdUser())
          .orElseThrow(
              () ->
                  new ModelNotFoundException(
                      String.format(
                          "User %d not found in company %d",
                          request.getIdUser(), request.getIdCompany())));

      // 3. Check if user has super admin role (bypass all checks)
      List<UserCompanyRole> userRoles = userCompanyRoleRepository.findByUserCompany(userCompany);

      if (isSuperAdmin(userRoles)) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information("Super admin detected - full access granted"),
            REQUEST,
            LEVEL_001);
        return FeatureValidationResponseDto.allowed("Access granted - Super Admin privileges");
      }

      // 4. Validate ROLE permission (Layer 1)
      if (!hasRolePermission(userRoles, feature)) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information("Role permission DENIED - no matching roles found"),
            RESPONSE,
            LEVEL_002);
        return FeatureValidationResponseDto.denied(
            FeatureValidationResponseDto.REASON_NO_ROLE,
            "User's role does not have permission to access this feature");
      }

      // 5. Call ebill-billing to validate subscription (Layer 2)
      SubscriptionResponseDto subscription;
      try {
        subscription = billingClient.getCurrentSubscription(request.getIdCompany());
      } catch (Exception e) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information("Error calling billing service: " + e.getMessage()),
            RESPONSE,
            LEVEL_002);

        return FeatureValidationResponseDto.denied(
            FeatureValidationResponseDto.REASON_NO_SUBSCRIPTION,
            "Unable to verify subscription. Please contact support.");
      }

      if (Objects.isNull(subscription)) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information("Billing service returned null subscription"),
            RESPONSE,
            LEVEL_002);
        return FeatureValidationResponseDto.deniedWithUpgrade(
            FeatureValidationResponseDto.REASON_NO_SUBSCRIPTION,
            "No active subscription found for this company",
            "/subscriptions/plans");
      }

      // Check if subscription has essential fields null (empty response)
      // Note: subscription is guaranteed to be non-null here due to check above
      if (Objects.isNull(subscription.getStatus()) && Objects.isNull(subscription.getPlanCode())) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information("Billing service returned subscription with null fields - likely no subscription exists"),
            RESPONSE,
            LEVEL_002);
        return FeatureValidationResponseDto.deniedWithUpgrade(
            FeatureValidationResponseDto.REASON_NO_SUBSCRIPTION,
            "No active subscription found for this company",
            "/subscriptions/plans");
      }

      // 6. Validate subscription status
      FeatureValidationResponseDto subscriptionValidation = validateSubscriptionStatus(subscription);
      if (subscriptionValidation != null) {
        return subscriptionValidation;
      }

      // 7. Validate plan includes this feature
      FeatureValidationResponseDto planValidation = validatePlanFeature(subscription, feature);
      if (planValidation != null) {
        return planValidation;
      }

      // 8. Check usage limits (if applicable)
      FeatureValidationResponseDto limitValidation = validateUsageLimits(request.getFeatureCode(), subscription);
      if (limitValidation != null) {
        return limitValidation;
      }

      // All validations passed!
      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information("Feature validation successful - access granted"),
          REQUEST,
          LEVEL_001);

      return FeatureValidationResponseDto.allowed(
          String.format(
              "Access granted to feature '%s' under plan '%s'",
              feature.getFeatureName(), subscription.getPlanName()));

    } catch (ModelNotFoundException e) {
      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information(e.getMessage()),
          RESPONSE,
          LEVEL_002);
      throw e;
    } catch (Exception e) {
      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information("Error validating feature access: " + e.getMessage()),
          RESPONSE,
          LEVEL_002);
      return FeatureValidationResponseDto.denied(
          "ERROR", "An error occurred while validating feature access");
    }
  }

  // ============================================
  // ROLE PERMISSION MANAGEMENT (New methods)
  // ============================================

  @Override
  public List<FeaturePermissionDto> getAllFeaturesForManagement() {
    String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method(methodName)
            .information("Fetching features for management (only leaf menus)"),
        REQUEST,
        LEVEL_001);

    List<String> leafMenuFeatureCodes = menuRepository.findDistinctFeatureCodesFromLeafMenus();
    List<FeaturePermission> allFeatures = featurePermissionRepository.findAll();
    
    List<FeaturePermissionDto> featureDtos = filterAndMapFeaturesForLeafMenus(
        allFeatures, leafMenuFeatureCodes);

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method(methodName)
            .information(
                String.format(
                    "Found %d features with leaf menus (filtered from %d total)",
                    featureDtos.size(),
                    allFeatures.size())),
        RESPONSE,
        LEVEL_001);

    return featureDtos;
  }

  /**
   * Filter features that have leaf menus associated and map them to DTOs.
   *
   * @param allFeatures All features from repository
   * @param leafMenuFeatureCodes List of feature codes from leaf menus
   * @return Filtered and mapped feature DTOs
   */
  private List<FeaturePermissionDto> filterAndMapFeaturesForLeafMenus(
      List<FeaturePermission> allFeatures, List<String> leafMenuFeatureCodes) {
    return allFeatures.stream()
        .filter(feature -> isFeatureEligibleForManagement(feature, leafMenuFeatureCodes))
        .map(this::toFeaturePermissionDto)
        .toList();
  }

  /**
   * Check if a feature is eligible for management (has leaf menu and is active).
   *
   * @param feature Feature to check
   * @param leafMenuFeatureCodes List of feature codes from leaf menus
   * @return true if feature is eligible
   */
  private boolean isFeatureEligibleForManagement(
      FeaturePermission feature, List<String> leafMenuFeatureCodes) {
    return feature.getFeatureCode() != null
        && leafMenuFeatureCodes.contains(feature.getFeatureCode())
        && Boolean.TRUE.equals(feature.getIsActive());
  }

  /**
   * Convert FeaturePermission entity to FeaturePermissionDto.
   *
   * @param feature FeaturePermission entity
   * @return FeaturePermissionDto
   */
  private FeaturePermissionDto toFeaturePermissionDto(FeaturePermission feature) {
    return FeaturePermissionDto.builder()
        .featureCode(feature.getFeatureCode())
        .featureName(feature.getFeatureName())
        .description(feature.getDescription())
        .module(feature.getModule())
        .allowedRoles(
            feature.getAllowedRoles() != null
                ? Arrays.asList(feature.getAllowedRoles())
                : List.of())
        .requiredPlans(
            feature.getRequiredPlans() != null
                ? Arrays.asList(feature.getRequiredPlans())
                : List.of())
        .isActive(feature.getIsActive())
        .build();
  }

  @Override
  public RolePermissionsDto getRolePermissions(Integer roleId) {
    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("getRolePermissions")
            .information(String.format("Fetching permissions for role %d", roleId)),
        REQUEST,
        LEVEL_001);

    // Get features for this role
    List<FeaturePermission> features = featurePermissionRepository.findFeaturesByRole(roleId);

    // Convert to DTOs
    List<FeaturePermissionDto> featureDtos =
        features.stream()
            .map(this::toFeaturePermissionDto)
            .toList();

    // Build response
    RolePermissionsDto response =
        RolePermissionsDto.builder()
            .roleId(roleId)
            .roleName(getRoleName(roleId))
            .permissions(featureDtos)
            .totalFeatures(featureDtos.size())
            .build();

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("getRolePermissions")
            .information(
                String.format(
                    "Found %d features for role %d", featureDtos.size(), roleId)),
        RESPONSE,
        LEVEL_001);

    return response;
  }

  @Override
  @Transactional
  public FeaturePermissionDto updateFeatureRoles(
      String featureCode, UpdateFeatureRolesDto updateDto) {
    String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method(methodName)
            .information(
                String.format(
                    "Updating allowed roles for feature %s: %s",
                    featureCode, updateDto.getAllowedRoles())),
        REQUEST,
        LEVEL_001);

    // Find the feature
    FeaturePermission feature =
        featurePermissionRepository
            .findByFeatureCode(featureCode)
            .orElseThrow(
                () ->
                    new ModelNotFoundException("Feature not found with code: " + featureCode));

    // Update allowed roles
    Integer[] rolesArray = updateDto.getAllowedRoles().toArray(new Integer[0]);
    feature.setAllowedRoles(rolesArray);

    // Save
    FeaturePermission updated = featurePermissionRepository.save(feature);
    flushAndClearCache();

    // Synchronize menu_role with allowed_roles
    syncMenuRoleForFeature(featureCode, rolesArray);

    // Convert to DTO
    FeaturePermissionDto response = toFeaturePermissionDto(updated);

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("updateFeatureRoles")
            .information(String.format("Feature %s updated successfully", featureCode)),
        RESPONSE,
        LEVEL_001);

    return response;
  }


  @Override
  @Transactional
  public List<FeaturePermissionDto> bulkUpdateRoles(BulkUpdateRolesDto bulkDto) {
    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("bulkUpdateRoles")
            .information(
                String.format("Bulk updating roles for %d features", bulkDto.getFeatureRoles().size())),
        REQUEST,
        LEVEL_001);

    List<FeaturePermissionDto> updatedFeatures = new java.util.ArrayList<>();

    for (Map.Entry<String, List<Integer>> entry : bulkDto.getFeatureRoles().entrySet()) {
      String featureCode = entry.getKey();
      List<Integer> allowedRoles = entry.getValue();

      Optional<FeaturePermission> featureOpt =
          featurePermissionRepository.findByFeatureCode(featureCode);

      if (featureOpt.isPresent()) {
        FeaturePermission feature = featureOpt.get();
        Integer[] rolesArray = allowedRoles.toArray(new Integer[0]);
        feature.setAllowedRoles(rolesArray);
        FeaturePermission updated = featurePermissionRepository.save(feature);
        flushAndClearCache();

        // Synchronize menu_role with allowed_roles
        syncMenuRoleForFeature(featureCode, rolesArray);

        FeaturePermissionDto dto = toFeaturePermissionDto(updated);

        updatedFeatures.add(dto);
      } else {
        log.warn("Feature not found for bulk update: {}", featureCode);
      }
    }

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("bulkUpdateRoles")
            .information(
                String.format("Bulk updated %d features", updatedFeatures.size())),
        RESPONSE,
        LEVEL_001);

    return updatedFeatures;
  }

  @Override
  public FeaturePermissionDto getFeaturePlans(String featureCode) {
    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("getFeaturePlans")
            .information("featureCode: " + featureCode),
        REQUEST,
        LEVEL_001);

    FeaturePermission feature =
        featurePermissionRepository
            .findByFeatureCode(featureCode)
            .orElseThrow(
                () ->
                    new ModelNotFoundException("Feature not found with code: " + featureCode));

    FeaturePermissionDto dto = toFeaturePermissionDto(feature);

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("getFeaturePlans")
            .information(String.format("Found plans for feature %s", featureCode)),
        RESPONSE,
        LEVEL_001);

    return dto;
  }

  @Override
  @Transactional
  public FeaturePermissionDto updateFeaturePlans(
      String featureCode, UpdateFeaturePlansDto updateDto) {
    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("updateFeaturePlans")
            .information(
                String.format(
                    "Updating required plans for feature %s: %s",
                    featureCode, updateDto.getRequiredPlans())),
        REQUEST,
        LEVEL_001);

    // Find the feature
    Optional<FeaturePermission> featureOpt = featurePermissionRepository.findByFeatureCode(featureCode);
    
    if (featureOpt.isEmpty()) {
      log.warn(
          "Feature '{}' not found. This may occur when trying to update plans for a menu without featureCode. "
              + "Menus without featureCode don't require plan updates.",
          featureCode);
      throw new ModelNotFoundException(
          String.format(
              "Feature not found with code: %s. If this menu doesn't have a featureCode, "
                  + "plan updates are not needed. Otherwise, create the feature first.",
              featureCode));
    }

    FeaturePermission feature = featureOpt.get();

    // Update required plans
    String[] plansArray = updateDto.getRequiredPlans().toArray(new String[0]);
    feature.setRequiredPlans(plansArray);

    // Save
    FeaturePermission updated = featurePermissionRepository.save(feature);
    flushAndClearCache();

    // Convert to DTO
    FeaturePermissionDto response = toFeaturePermissionDto(updated);

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method("updateFeaturePlans")
            .information(String.format("Feature %s plans updated successfully", featureCode)),
        RESPONSE,
        LEVEL_001);

    return response;
  }


  @Override
  public List<String> getUserAllowedFeatures(Long idUser, Long idCompany) {
    String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method(methodName)
            .information(
                String.format(
                    "Getting allowed features for user=%d, company=%d", idUser, idCompany)),
        REQUEST,
        LEVEL_001);

    try {
      // 1. Validate user belongs to company
      UserCompany userCompany =
          userCompanyRepo
              .findByIdCompanyAndUser_IdUser(idCompany, idUser)
              .orElseThrow(
                  () ->
                      new ModelNotFoundException(
                          String.format("User %d not found in company %d", idUser, idCompany)));

      // 2. Get user roles
      List<UserCompanyRole> userRoles = userCompanyRoleRepository.findByUserCompany(userCompany);

      // 3. Check if user is super admin (has access to all features)
      if (isSuperAdmin(userRoles)) {
        List<String> allFeatures =
            featurePermissionRepository.findByIsActiveTrue().stream()
                .map(FeaturePermission::getFeatureCode)
                .toList();

        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information(
                    String.format(
                        "Super admin detected - returning all %d active features", allFeatures.size())),
            RESPONSE,
            LEVEL_001);
        return allFeatures;
      }

      // 4. Get active role IDs for the user
      List<Long> activeRoleIds = getActiveRoleIds(userRoles);

      if (activeRoleIds.isEmpty()) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information("User has no active roles - returning empty list"),
            RESPONSE,
            LEVEL_001);
        return List.of();
      }

      // 5. Get subscription once (cache for all features)
      SubscriptionResponseDto subscription = null;
      try {
        subscription = billingClient.getCurrentSubscription(idCompany);

        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information(
                    String.format(
                        "Subscription loaded: status=%s, planCode=%s, isActive=%s",
                        Objects.nonNull(subscription) ? subscription.getStatus() : "null",
                        Objects.nonNull(subscription) ? subscription.getPlanCode() : "null",
                        Objects.nonNull(subscription) ? subscription.getIsActive() : "null")),
            RESPONSE,
            LEVEL_001);
      } catch (Exception e) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information("Error calling billing service: " + e.getMessage()),
            RESPONSE,
            LEVEL_002);
        // If billing service fails, we can't validate plan permissions
        // Return only role-based features
      }

      // 6. Get all active features
      List<FeaturePermission> allFeatures = featurePermissionRepository.findByIsActiveTrue();

      // 7. Filter features based on role and plan
      List<String> allowedFeatures = filterAllowedFeatures(allFeatures, activeRoleIds, subscription);

      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information(
                  String.format(
                      "Found %d allowed features out of %d total features: %s",
                      allowedFeatures.size(),
                      allFeatures.size(),
                      allowedFeatures)),
              RESPONSE,
              LEVEL_001);

      return allowedFeatures;

    } catch (ModelNotFoundException e) {
      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information(e.getMessage()),
          RESPONSE,
          LEVEL_002);
      throw e;
    } catch (Exception e) {
      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information("Error getting user features: " + e.getMessage()),
          RESPONSE,
          LEVEL_002);
      // Return empty list on error instead of throwing
      return List.of();
    }
  }

  /**
   * Get role name by ID from database.
   * Returns a default name if role is not found.
   *
   * @param roleId Role ID to lookup
   * @return Role name or default name if not found
   */
  private String getRoleName(Integer roleId) {
    return roleRepository
        .findById(roleId.longValue())
        .map(Role::getName)
        .orElse("Role " + roleId);
  }

  /**
   * Check if user has super admin role.
   *
   * @param userRoles List of user company roles
   * @return true if user is super admin
   */
  private boolean isSuperAdmin(List<UserCompanyRole> userRoles) {
    return userRoles.stream()
        .filter(UserCompanyRole::isStatus)
        .anyMatch(
            ucr ->
                ucr.getRole().getIdRole().equals(Long.valueOf(SUPER_ADMIN_ROLE_ID))
                    || SUPER_ADMIN_ROLE.equals(ucr.getRole().getName()));
  }

  /**
   * Check if user has role permission for a feature.
   *
   * @param userRoles List of user company roles
   * @param feature Feature to check permission for
   * @return true if user has role permission
   */
  private boolean hasRolePermission(List<UserCompanyRole> userRoles, FeaturePermission feature) {
    if (feature.getAllowedRoles() == null || feature.getAllowedRoles().length == 0) {
      return false;
    }

    return userRoles.stream()
        .filter(UserCompanyRole::isStatus)
        .anyMatch(userRole -> {
          Long roleId = userRole.getRole().getIdRole();
          return Arrays.stream(feature.getAllowedRoles())
              .anyMatch(allowedRoleId ->
                  allowedRoleId != null && allowedRoleId.equals(roleId.intValue()));
        });
  }

  /**
   * Get active role IDs from user roles.
   *
   * @param userRoles List of user company roles
   * @return List of active role IDs
   */
  private List<Long> getActiveRoleIds(List<UserCompanyRole> userRoles) {
    return userRoles.stream()
        .filter(UserCompanyRole::isStatus)
        .map(ucr -> ucr.getRole().getIdRole())
        .toList();
  }

  /**
   * Validate subscription status and return denial response if invalid.
   *
   * @param subscription Subscription to validate
   * @return Denial response if invalid, null if valid
   */
  private FeatureValidationResponseDto validateSubscriptionStatus(SubscriptionResponseDto subscription) {
    if (!subscription.isValidSubscription()) {
      if (SUBSCRIPTION_STATUS_EXPIRED.equalsIgnoreCase(subscription.getStatus())) {
        return FeatureValidationResponseDto.deniedWithUpgrade(
            FeatureValidationResponseDto.REASON_EXPIRED,
            "Your subscription has expired. Please renew to continue using this feature.",
            URL_SUBSCRIPTIONS_RENEW);
      }
      return FeatureValidationResponseDto.deniedWithUpgrade(
          FeatureValidationResponseDto.REASON_NO_SUBSCRIPTION,
          "Subscription is not active. Status: " + subscription.getStatus(),
          URL_SUBSCRIPTIONS_PLANS);
    }
    return null;
  }

  /**
   * Validate if plan includes the feature and return denial response if not.
   *
   * @param subscription Subscription to check
   * @param feature Feature to validate
   * @return Denial response if plan doesn't include feature, null if valid
   */
  private FeatureValidationResponseDto validatePlanFeature(
      SubscriptionResponseDto subscription, FeaturePermission feature) {
    if (!subscription.hasPlanFeature(feature.getRequiredPlans())) {
      return FeatureValidationResponseDto.deniedWithUpgrade(
          FeatureValidationResponseDto.REASON_PLAN_NOT_ALLOWED,
          String.format(
              "Your current plan (%s) does not include this feature. Upgrade to access it.",
              subscription.getPlanName()),
          URL_SUBSCRIPTIONS_UPGRADE);
    }
    return null;
  }

  /**
   * Validate usage limits for invoice features and return denial response if limit exceeded.
   *
   * @param featureCode Feature code to check
   * @param subscription Subscription to check limits
   * @return Denial response if limit exceeded, null if valid
   */
  private FeatureValidationResponseDto validateUsageLimits(
      String featureCode, SubscriptionResponseDto subscription) {
    if (featureCode.startsWith(FEATURE_CODE_PREFIX_INVOICE)
        && subscription.hasReachedInvoiceLimit()) {
      return FeatureValidationResponseDto.deniedWithUpgrade(
          FeatureValidationResponseDto.REASON_LIMIT_EXCEEDED,
          String.format(
              "Invoice limit reached (%d/%d). Upgrade your plan for more invoices.",
              subscription.getInvoiceUsed(), subscription.getInvoiceLimit()),
          URL_SUBSCRIPTIONS_UPGRADE);
    }
    return null;
  }

  /**
   * Filter features based on role and plan permissions.
   *
   * @param allFeatures All features to filter
   * @param activeRoleIds Active role IDs for the user
   * @param subscription Subscription for plan validation (can be null)
   * @return List of allowed feature codes
   */
  private List<String> filterAllowedFeatures(
      List<FeaturePermission> allFeatures,
      List<Long> activeRoleIds,
      SubscriptionResponseDto subscription) {
    return allFeatures.stream()
        .filter(feature -> hasRolePermissionForFeature(feature, activeRoleIds))
        .filter(feature -> hasPlanPermissionForFeature(feature, subscription))
        .map(FeaturePermission::getFeatureCode)
        .toList();
  }

  /**
   * Check if any active role has permission for the feature.
   *
   * @param feature Feature to check
   * @param activeRoleIds Active role IDs
   * @return true if any role has permission
   */
  private boolean hasRolePermissionForFeature(FeaturePermission feature, List<Long> activeRoleIds) {
    if (feature.getAllowedRoles() == null || feature.getAllowedRoles().length == 0) {
      return false;
    }

    return activeRoleIds.stream()
        .anyMatch(roleId ->
            Arrays.stream(feature.getAllowedRoles())
                .anyMatch(allowedRoleId ->
                    allowedRoleId != null && allowedRoleId.equals(roleId.intValue())));
  }

  /**
   * Check if feature has plan permission based on subscription.
   *
   * @param feature Feature to check
   * @param subscription Subscription (can be null)
   * @return true if plan permission is valid
   */
  private boolean hasPlanPermissionForFeature(
      FeaturePermission feature, SubscriptionResponseDto subscription) {
    if (Objects.isNull(subscription)) {
      // If billing service is unavailable, only return features that don't require plans
      return feature.getRequiredPlans() == null || feature.getRequiredPlans().length == 0;
    }

    if (!subscription.isValidSubscription()) {
      return false;
    }

    return subscription.hasPlanFeature(feature.getRequiredPlans());
  }

  /**
   * Force flush to database and clear cache to ensure fresh data on next read.
   */
  private void flushAndClearCache() {
    entityManager.flush();
    entityManager.clear();
  }

  /**
   * Synchronize menu_role when allowed_roles of a feature are updated.
   * This ensures that menus appear in the sidebar when permissions are granted.
   *
   * @param featureCode Feature code to synchronize
   * @param allowedRoles Array of role IDs that should have access to this feature
   */
  @Transactional
  private void syncMenuRoleForFeature(String featureCode, Integer[] allowedRoles) {
    String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

    setLogs(
        EventLog.builder()
            .service(this.getClass().getName())
            .method(methodName)
            .information(
                String.format(
                    "Synchronizing menu_role for feature %s with roles: %s",
                    featureCode, Arrays.toString(allowedRoles))),
        REQUEST,
        LEVEL_001);

    try {
      List<Menu> menusToSync = findMenusForFeature(featureCode);
      
      if (menusToSync.isEmpty()) {
        setLogs(
            EventLog.builder()
                .service(this.getClass().getName())
                .method(methodName)
                .information(
                    String.format("No active menus found with feature_code: %s", featureCode)),
            RESPONSE,
            LEVEL_001);
        return;
      }

      int totalMenuRoles = 0;
      for (Menu menu : menusToSync) {
        syncMenuRoles(menu, allowedRoles);
        totalMenuRoles += menuRoleRepository.findByMenu_IdMenu(menu.getIdMenu()).size();
      }

      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information(
                  String.format(
                      "Successfully synchronized %d menus for feature %s - Total menu_role entries: %d",
                      menusToSync.size(), featureCode, totalMenuRoles)),
          RESPONSE,
          LEVEL_001);

    } catch (Exception e) {
      log.error(
          "Error synchronizing menu_role for feature {}: {}",
          featureCode,
          e.getMessage(),
          e);
      setLogs(
          EventLog.builder()
              .service(this.getClass().getName())
              .method(methodName)
              .information("Error synchronizing menu_role: " + e.getMessage()),
          RESPONSE,
          LEVEL_002);
      // Don't throw exception - menu_role sync failure shouldn't break feature update
    }
  }

  /**
   * Find all menus associated with a feature code, including related features dynamically.
   * If a feature doesn't have direct menus, it searches for related features in the same module
   * that do have menus associated.
   *
   * @param featureCode Feature code to search for
   * @return List of active menus associated with the feature or related features
   */
  private List<Menu> findMenusForFeature(String featureCode) {
    // First, try to find direct menus for this feature
    List<Menu> menus = menuRepository.findByFeatureCodeAndStatus(featureCode, true);
    
    if (!menus.isEmpty()) {
      return menus;
    }

    // If no direct menus found, find related features dynamically
    List<String> relatedFeatureCodes = findRelatedFeatureCodes(featureCode);
    
    return relatedFeatureCodes.stream()
        .flatMap(relatedCode -> menuRepository.findByFeatureCodeAndStatus(relatedCode, true).stream())
        .toList();
  }

  /**
   * Find related feature codes dynamically based on module or feature prefix.
   * This makes the system dynamic - any new features in the same module will automatically
   * be included without code changes.
   *
   * @param featureCode Feature code to find related features for
   * @return List of related feature codes that have menus associated
   */
  private List<String> findRelatedFeatureCodes(String featureCode) {
    // Get the parent feature to determine its module
    Optional<FeaturePermission> parentFeature = 
        featurePermissionRepository.findByFeatureCode(featureCode);
    
    if (parentFeature.isEmpty()) {
      return List.of();
    }

    String module = parentFeature.get().getModule();
    if (module == null || module.isBlank()) {
      // If no module, try to find by feature prefix (e.g., "REPORT_" for "REPORT_OPERATIONAL")
      String prefix = extractFeaturePrefix(featureCode);
      if (prefix == null) {
        return List.of();
      }
      return findFeaturesByPrefix(prefix);
    }

    // Find all features in the same module that have menus
    List<FeaturePermission> moduleFeatures = 
        featurePermissionRepository.findByModuleAndIsActiveTrue(module);
    
    return moduleFeatures.stream()
        .map(FeaturePermission::getFeatureCode)
        .filter(code -> !code.equals(featureCode)) // Exclude the parent feature itself
        .filter(code -> hasMenus(code)) // Only include features that have menus
        .toList();
  }

  /**
   * Extract feature prefix from feature code (e.g., "REPORT_" from "REPORT_OPERATIONAL").
   *
   * @param featureCode Feature code
   * @return Prefix before underscore, or null if no underscore found
   */
  private String extractFeaturePrefix(String featureCode) {
    if (featureCode == null || !featureCode.contains("_")) {
      return null;
    }
    int underscoreIndex = featureCode.indexOf("_");
    return featureCode.substring(0, underscoreIndex + 1); // Include the underscore
  }

  /**
   * Find features that start with a specific prefix and have menus associated.
   *
   * @param prefix Feature prefix (e.g., "REPORT_")
   * @return List of feature codes with the prefix that have menus
   */
  private List<String> findFeaturesByPrefix(String prefix) {
    return featurePermissionRepository.findByIsActiveTrue().stream()
        .map(FeaturePermission::getFeatureCode)
        .filter(code -> code != null && code.startsWith(prefix))
        .filter(this::hasMenus)
        .toList();
  }

  /**
   * Check if a feature code has menus associated.
   *
   * @param featureCode Feature code to check
   * @return true if feature has menus
   */
  private boolean hasMenus(String featureCode) {
    return !menuRepository.findByFeatureCodeAndStatus(featureCode, true).isEmpty();
  }

  /**
   * Synchronize menu_role entries for a specific menu.
   * Deletes existing entries and creates new ones for the allowed roles.
   *
   * @param menu Menu to synchronize
   * @param allowedRoles Array of role IDs that should have access
   */
  private void syncMenuRoles(Menu menu, Integer[] allowedRoles) {
    // Delete existing menu_role entries
    menuRoleRepository.deleteByMenuId(menu.getIdMenu());
    entityManager.flush();

    // Create new menu_role entries for each allowed role
    for (Integer roleId : allowedRoles) {
      createMenuRoleIfNotExists(menu, roleId);
    }
    
    entityManager.flush();
  }

  /**
   * Create a menu_role entry if it doesn't already exist.
   *
   * @param menu Menu to associate with the role
   * @param roleId Role ID to associate with the menu
   */
  private void createMenuRoleIfNotExists(Menu menu, Integer roleId) {
    try {
      if (menuRoleRepository.existsByMenu_IdMenuAndRole_IdRole(menu.getIdMenu(), roleId)) {
        return;
      }

      Role role = roleRepository
          .findById(roleId.longValue())
          .orElseThrow(() -> new ModelNotFoundException("Role not found with ID: " + roleId));

      MenuRole menuRole = new MenuRole();
      menuRole.setMenu(menu);
      menuRole.setRole(role);
      menuRole.setStatus(true);
      menuRoleRepository.save(menuRole);

    } catch (ModelNotFoundException e) {
      log.warn("Role not found: {} - skipping menu_role creation", roleId);
    } catch (Exception e) {
      log.error(
          "Error creating menu_role for menuId={}, roleId={}: {}",
          menu.getIdMenu(),
          roleId,
          e.getMessage());
    }
  }
}
