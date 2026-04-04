package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.dto.FeaturePermissionDto;
import com.wallparisoft.authentication.dto.MenuRequestDto;
import com.wallparisoft.authentication.dto.MenuResponseDto;
import com.wallparisoft.authentication.entity.FeaturePermission;
import com.wallparisoft.authentication.entity.Menu;
import com.wallparisoft.authentication.entity.Role;
import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.entity.UserCompanyRole;
import com.wallparisoft.authentication.exception.ModelNotFoundException;
import com.wallparisoft.authentication.repository.IFeaturePermissionRepository;
import com.wallparisoft.authentication.repository.IMenuRepository;
import com.wallparisoft.authentication.repository.IUserCompanyRepo;
import com.wallparisoft.authentication.repository.IUserCompanyRoleRepository;
import com.wallparisoft.authentication.repository.IUserRepository;
import com.wallparisoft.authentication.service.IMenuAdminService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class MenuAdminServiceImpl implements IMenuAdminService {

  private static final Integer SUPER_ADMIN_ROLE_ID = 1;
  private static final String SUPER_ADMIN_ROLE = "super_admin";

  private final IMenuRepository menuRepository;
  private final IUserRepository userRepository;
  private final IUserCompanyRepo userCompanyRepo;
  private final IUserCompanyRoleRepository userCompanyRoleRepository;
  private final IFeaturePermissionRepository featurePermissionRepository;

  @Override
  public List<MenuResponseDto> getAllMenus() {
    List<Menu> allMenus = menuRepository.findAll();
    return buildMenuTree(allMenus);
  }

  @Override
  public MenuResponseDto getMenuById(Integer id) {
    Menu menu =
        menuRepository
            .findById(id.longValue())
            .orElseThrow(() -> new ModelNotFoundException("Menú no encontrado con ID: " + id));
    return convertToResponseDto(menu);
  }

  @Override
  @Transactional
  public MenuResponseDto createMenu(MenuRequestDto request) {
    // Validar campos requeridos para crear
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new IllegalArgumentException("El nombre del menú es requerido");
    }
    if (request.getIcon() == null || request.getIcon().trim().isEmpty()) {
      throw new IllegalArgumentException("El icono es requerido");
    }
    if (request.getActive() == null) {
      throw new IllegalArgumentException("El estado activo es requerido");
    }

    // Validar parentId si se proporciona
    if (request.getParentId() != null) {
      menuRepository
          .findById(request.getParentId().longValue())
          .orElseThrow(
              () ->
                  new ModelNotFoundException(
                      "Menú padre no encontrado con ID: " + request.getParentId()));
    }

    // Validar featureCode si se proporciona
    if (request.getFeatureCode() != null && !request.getFeatureCode().isEmpty()) {
      featurePermissionRepository
          .findByFeatureCodeAndIsActive(request.getFeatureCode(), true)
          .orElseThrow(
              () ->
                  new ModelNotFoundException(
                      "Feature code no encontrado o inactivo: " + request.getFeatureCode()));
    }

    Menu menu = new Menu();
    menu.setName(request.getName().trim());
    menu.setIcon(request.getIcon().trim());
    // URL puede ser null, vacía o comenzar con /
    menu.setUrl(
        request.getUrl() != null && !request.getUrl().isEmpty() && !request.getUrl().equals("null")
            ? request.getUrl()
            : null);
    menu.setFeatureCode(request.getFeatureCode());
    menu.setStatus(request.getActive());
    menu.setOrder(request.getOrder());

    if (request.getParentId() != null) {
      Menu parent =
          menuRepository
              .findById(request.getParentId().longValue())
              .orElseThrow(
                  () ->
                      new ModelNotFoundException(
                          "Menú padre no encontrado con ID: " + request.getParentId()));
      menu.setParent(parent);
    }

    Menu savedMenu = menuRepository.save(menu);

    // Asignar automáticamente el menú creado a super_admin (role 1)
    // Usa la función helper assign_menu_to_role_safe que es idempotente
    menuRepository.assignMenuToRole(savedMenu.getIdMenu(), SUPER_ADMIN_ROLE_ID);

    return convertToResponseDto(savedMenu);
  }

  @Override
  @Transactional
  public MenuResponseDto updateMenu(Integer id, MenuRequestDto request) {
    Menu menu =
        menuRepository
            .findById(Long.valueOf(id))
            .orElseThrow(() -> new ModelNotFoundException("Menú no encontrado con ID: " + id));

    // Actualización parcial (PATCH): solo actualizar campos que vienen en el request

    // Actualizar name si viene en el request
    if (request.getName() != null) {
      if (request.getName().trim().isEmpty()) {
        throw new IllegalArgumentException("El nombre del menú no puede estar vacío");
      }
      menu.setName(request.getName().trim());
    }

    // Actualizar icon si viene en el request
    if (request.getIcon() != null) {
      if (request.getIcon().trim().isEmpty()) {
        throw new IllegalArgumentException("El icono no puede estar vacío");
      }
      menu.setIcon(request.getIcon().trim());
    }

    // Actualizar url si viene en el request (puede ser null para limpiar)
    if (request.getUrl() != null) {
      menu.setUrl(
          request.getUrl().isEmpty() || request.getUrl().equals("null") ? null : request.getUrl());
    }

    // Actualizar parentId si viene en el request
    if (request.getParentId() != null) {
      // Si se envía 0 o -1, significa que se quiere quitar el padre
      if (request.getParentId() <= 0) {
        menu.setParent(null);
      } else {
        Menu parent =
            menuRepository
                .findById(request.getParentId().longValue())
                .orElseThrow(
                    () ->
                        new ModelNotFoundException(
                            "Menú padre no encontrado con ID: " + request.getParentId()));

        // Validar que no se cree un ciclo (el padre no puede ser un descendiente)
        if (isDescendant(parent.getIdMenu(), id)) {
          throw new IllegalArgumentException(
              "No se puede asignar un descendiente como padre del menú");
        }

        menu.setParent(parent);
      }
    }

    // Actualizar featureCode si viene en el request
    if (request.getFeatureCode() != null) {
      if (request.getFeatureCode().isEmpty()) {
        // Si viene vacío, quitar el feature code
        menu.setFeatureCode(null);
      } else {
        // Validar que el feature existe y está activo
        featurePermissionRepository
            .findByFeatureCodeAndIsActive(request.getFeatureCode(), true)
            .orElseThrow(
                () ->
                    new ModelNotFoundException(
                        "Feature code no encontrado o inactivo: " + request.getFeatureCode()));
        menu.setFeatureCode(request.getFeatureCode());
      }
    }

    // Actualizar active (status) si viene en el request
    if (request.getActive() != null) {
      menu.setStatus(request.getActive());
    }

    // Actualizar order si viene en el request
    if (request.getOrder() != null) {
      menu.setOrder(request.getOrder());
    }

    Menu updatedMenu = menuRepository.save(menu);
    return convertToResponseDto(updatedMenu);
  }

  @Override
  @Transactional
  public void hardDeleteMenu(Integer id) {
    Menu menu =
        menuRepository
            .findById(id.longValue())
            .orElseThrow(() -> new ModelNotFoundException("Menú no encontrado con ID: " + id));

    // Paso 1: Buscar y eliminar recursivamente todos los submenús primero
    List<Menu> subMenus = menuRepository.findByParent_IdMenu(id);
    for (Menu subMenu : subMenus) {
      hardDeleteMenu(subMenu.getIdMenu());
    }

    // Paso 2: Eliminar todos los registros de menu_role asociados a este menú
    int deletedMenuRoles = menuRepository.deleteMenuRoleByMenuId(id);
    log.debug("Eliminados {} registros de menu_role para el menú ID: {}", deletedMenuRoles, id);

    // Paso 3: Si el menú tiene un feature_code, verificar si hay otros menús usándolo
    String featureCode = menu.getFeatureCode();
    if (featureCode != null && !featureCode.isEmpty()) {
      int otherMenusUsingFeature = menuRepository.countMenusUsingFeatureCode(featureCode, id);
      if (otherMenusUsingFeature == 0) {
        // Solo este menú usa el feature_code, eliminar el feature_permission
        featurePermissionRepository
            .findByFeatureCode(featureCode)
            .ifPresent(
                feature -> {
                  featurePermissionRepository.delete(feature);
                  log.debug(
                      "Eliminado feature_permission con código: {} (solo era usado por el menú ID: {})",
                      featureCode,
                      id);
                });
      } else {
        log.debug(
            "No se elimina feature_permission '{}' porque {} otros menús lo están usando",
            featureCode,
            otherMenusUsingFeature);
      }
    }

    // Paso 4: Eliminar el menú físicamente
    menuRepository.delete(menu);
    log.info("Menú eliminado físicamente: ID={}, nombre={}", id, menu.getName());
  }

  @Override
  public boolean isSuperAdmin(String userName) {
    User user =
        userRepository
            .findByUserName(userName)
            .orElseThrow(() -> new ModelNotFoundException("Usuario no encontrado: " + userName));

    List<UserCompany> userCompanies = userCompanyRepo.findByUser(user);
    if (userCompanies == null || userCompanies.isEmpty()) {
      return false;
    }

    for (UserCompany userCompany : userCompanies) {
      List<UserCompanyRole> userRoles = userCompanyRoleRepository.findByUserCompany(userCompany);
      for (UserCompanyRole userRole : userRoles) {
        if (!userRole.isStatus()) {
          continue;
        }
        Role role = userRole.getRole();
        if (role.getIdRole().equals(Long.valueOf(SUPER_ADMIN_ROLE_ID))
            || SUPER_ADMIN_ROLE.equals(role.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  /** Construye el árbol de menús desde una lista plana */
  private List<MenuResponseDto> buildMenuTree(List<Menu> menus) {
    Map<Integer, MenuResponseDto> menuMap = new HashMap<>();
    List<MenuResponseDto> rootMenus = new ArrayList<>();

    // Primera pasada: crear todos los DTOs sin submenús (conversión plana)
    for (Menu menu : menus) {
      MenuResponseDto dto = convertToResponseDtoFlat(menu);
      menuMap.put(menu.getIdMenu(), dto);
    }

    // Segunda pasada: construir la jerarquía
    for (Menu menu : menus) {
      MenuResponseDto dto = menuMap.get(menu.getIdMenu());
      if (menu.getParent() == null) {
        rootMenus.add(dto);
      } else {
        MenuResponseDto parentDto = menuMap.get(menu.getParent().getIdMenu());
        if (parentDto != null) {
          if (parentDto.getSubMenus() == null) {
            parentDto.setSubMenus(new ArrayList<>());
          }
          parentDto.getSubMenus().add(dto);
        }
      }
    }

    // Ordenar por order si está disponible
    sortMenusByOrder(rootMenus);
    return rootMenus;
  }

  /** Ordena los menús recursivamente por el campo order */
  private void sortMenusByOrder(List<MenuResponseDto> menus) {
    if (menus == null || menus.isEmpty()) {
      return;
    }
    menus.sort(
        (m1, m2) -> {
          Integer order1 = m1.getOrder() != null ? m1.getOrder() : Integer.MAX_VALUE;
          Integer order2 = m2.getOrder() != null ? m2.getOrder() : Integer.MAX_VALUE;
          return order1.compareTo(order2);
        });
    menus.forEach(menu -> sortMenusByOrder(menu.getSubMenus()));
  }

  /** Convierte FeaturePermission a FeaturePermissionDto */
  private FeaturePermissionDto convertToFeaturePermissionDto(FeaturePermission feature) {
    if (feature == null) {
      return null;
    }
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

  /** Carga el FeaturePermission completo basado en el featureCode del menú */
  private FeaturePermissionDto loadFeaturePermission(String featureCode) {
    if (featureCode == null || featureCode.isEmpty()) {
      return null;
    }
    return featurePermissionRepository
        .findByFeatureCode(featureCode)
        .map(this::convertToFeaturePermissionDto)
        .orElse(null);
  }

  /**
   * Convierte una entidad Menu a MenuResponseDto de forma plana (sin submenús recursivos) Usado en
   * buildMenuTree para evitar duplicados
   */
  private MenuResponseDto convertToResponseDtoFlat(Menu menu) {
    MenuResponseDto dto = new MenuResponseDto();
    dto.setId(menu.getIdMenu());
    dto.setName(menu.getName());
    dto.setIcon(menu.getIcon());
    dto.setUrl(menu.getUrl());
    dto.setFeatureCode(menu.getFeatureCode());
    dto.setFeature(loadFeaturePermission(menu.getFeatureCode())); // Cargar feature completo
    dto.setParentId(menu.getParent() != null ? menu.getParent().getIdMenu() : null);
    dto.setActive(menu.getStatus());
    dto.setOrder(menu.getOrder());
    // Inicializar lista vacía - la jerarquía se construye en buildMenuTree
    dto.setSubMenus(new ArrayList<>());
    return dto;
  }

  /**
   * Convierte una entidad Menu a MenuResponseDto con conversión recursiva de submenús Usado cuando
   * se necesita un menú individual con su árbol completo
   */
  private MenuResponseDto convertToResponseDto(Menu menu) {
    MenuResponseDto dto = convertToResponseDtoFlat(menu);

    // Convertir submenús recursivamente solo si se necesita
    if (menu.getSubMenus() != null && !menu.getSubMenus().isEmpty()) {
      List<MenuResponseDto> subMenusDto =
          menu.getSubMenus().stream().map(this::convertToResponseDto).collect(Collectors.toList());
      dto.setSubMenus(subMenusDto);
    }

    return dto;
  }

  /** Verifica si un menú es descendiente de otro (para evitar ciclos) */
  private boolean isDescendant(Integer potentialParentId, Integer menuId) {
    Menu menu =
        menuRepository
            .findById(menuId.longValue())
            .orElseThrow(() -> new ModelNotFoundException("Menú no encontrado con ID: " + menuId));

    Menu current = menu.getParent();
    while (current != null) {
      if (current.getIdMenu().equals(potentialParentId)) {
        return true;
      }
      current = current.getParent();
    }
    return false;
  }
}
