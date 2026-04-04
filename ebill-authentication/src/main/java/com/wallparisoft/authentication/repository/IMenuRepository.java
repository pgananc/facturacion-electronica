package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.Menu;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IMenuRepository extends JpaRepository<Menu, Long> {

  @Query(
      value =
          """
        SELECT * FROM (
          -- Menús asignados directamente al rol del usuario
          SELECT DISTINCT m.id_menu, m.parent_id_menu, m.icon, m.name, m.url, m.status, m.feature_code, m.menu_order
          FROM user_company uc
          INNER JOIN users u ON u.id_user = uc.id_user
          INNER JOIN user_company_role ucr ON ucr.id_user_company = uc.id_user_company AND ucr.status = true
          INNER JOIN menu_role mr ON mr.id_role = ucr.id_role AND mr.status = true
          INNER JOIN menu m ON m.id_menu = mr.id_menu AND m.status = true
          WHERE u.username = :name

          UNION

          -- Menús padre que tienen submenús asignados al rol (necesarios para construir la jerarquía)
          SELECT DISTINCT parent.id_menu, parent.parent_id_menu, parent.icon, parent.name, parent.url, parent.status, parent.feature_code, parent.menu_order
          FROM user_company uc
          INNER JOIN users u ON u.id_user = uc.id_user
          INNER JOIN user_company_role ucr ON ucr.id_user_company = uc.id_user_company AND ucr.status = true
          INNER JOIN menu_role mr ON mr.id_role = ucr.id_role AND mr.status = true
          INNER JOIN menu child ON child.id_menu = mr.id_menu AND child.status = true
          INNER JOIN menu parent ON parent.id_menu = child.parent_id_menu AND parent.status = true
          WHERE u.username = :name
            AND parent.id_menu IS NOT NULL
        ) AS combined_menus
        ORDER BY COALESCE(menu_order, 999999), id_menu
        """,
      nativeQuery = true)
  List<Object[]> findMenuByUser(@Param("name") String name);

  /**
   * Asigna un menú a un rol de forma idempotente. Si ya existe una asignación activa, la actualiza.
   * Si no existe, crea una nueva. Esta implementación replica la lógica de assign_menu_to_role_safe
   * sin usar bloques DO.
   *
   * @param menuId ID del menú
   * @param roleId ID del rol (1 = super_admin)
   */
  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
          SELECT nextval('menu_role_seq'), :menuId, :roleId, true
          WHERE NOT EXISTS (
              SELECT 1 FROM public.menu_role
              WHERE id_menu = :menuId AND id_role = :roleId AND status = true
          );
          UPDATE public.menu_role
          SET status = true
          WHERE id_menu = :menuId AND id_role = :roleId AND status = false;
          """,
      nativeQuery = true)
  void assignMenuToRole(@Param("menuId") Integer menuId, @Param("roleId") Integer roleId);

  /**
   * Elimina físicamente todos los registros de menu_role asociados a un menú
   *
   * @param menuId ID del menú
   * @return Número de registros eliminados
   */
  @Transactional
  @Modifying
  @Query(value = "DELETE FROM public.menu_role WHERE id_menu = :menuId", nativeQuery = true)
  int deleteMenuRoleByMenuId(@Param("menuId") Integer menuId);

  /**
   * Cuenta cuántos menús (excluyendo el actual) están usando un feature_code
   *
   * @param featureCode Código del feature
   * @param excludeMenuId ID del menú a excluir del conteo
   * @return Número de menús usando el feature_code
   */
  @Query(
      value =
          "SELECT COUNT(*) FROM public.menu WHERE feature_code = :featureCode AND id_menu != :excludeMenuId",
      nativeQuery = true)
  int countMenusUsingFeatureCode(
      @Param("featureCode") String featureCode, @Param("excludeMenuId") Integer excludeMenuId);

  /**
   * Find all active menus with a specific feature_code
   *
   * @param featureCode Feature code to search for
   * @return List of menus with the feature code
   */
  List<Menu> findByFeatureCodeAndStatus(String featureCode, Boolean status);

  /**
   * Busca todos los submenús de un menú padre
   *
   * @param parentId ID del menú padre
   * @return Lista de submenús
   */
  List<Menu> findByParent_IdMenu(Integer parentId);

  /**
   * Find all leaf menus (menus that don't have children) with a specific feature_code
   * A leaf menu is one that:
   * - Has a feature_code assigned
   * - Does NOT have any submenus (no other menu has this menu's id as parent_id_menu)
   * - Is active
   *
   * @param featureCode Feature code to search for
   * @param status Active status (typically true)
   * @return List of leaf menus with the feature code
   */
  @Query(
      """
      SELECT m FROM Menu m
      WHERE m.featureCode = :featureCode
      AND m.status = :status
      AND NOT EXISTS (
          SELECT 1 FROM Menu child
          WHERE child.parent.idMenu = m.idMenu
          AND child.status = true
      )
      """)
  List<Menu> findLeafMenusByFeatureCodeAndStatus(
      @Param("featureCode") String featureCode, @Param("status") Boolean status);

  /**
   * Get all distinct feature_codes that are assigned to leaf menus (menus without children)
   * This is used to filter features that should appear in the management UI
   *
   * @return List of feature codes that have leaf menus associated
   */
  @Query(
      value =
          """
          SELECT DISTINCT m.feature_code
          FROM public.menu m
          WHERE m.feature_code IS NOT NULL
          AND m.status = true
          AND NOT EXISTS (
              SELECT 1 FROM public.menu child
              WHERE child.parent_id_menu = m.id_menu
              AND child.status = true
          )
          """,
      nativeQuery = true)
  List<String> findDistinctFeatureCodesFromLeafMenus();

  /**
   * Find all active menus without filtering by menu_role
   * Returns all menus with status = true, ordered by menu_order
   * Used when frontend handles permission filtering
   *
   * @return List of all active menus
   */
  @Query(
      value =
          """
          SELECT m.id_menu, m.parent_id_menu, m.icon, m.name, m.url, m.status, m.feature_code, m.menu_order
          FROM public.menu m
          WHERE m.status = true
          ORDER BY COALESCE(m.menu_order, 999999), m.id_menu
          """,
      nativeQuery = true)
  List<Object[]> findAllActiveMenus();
}
