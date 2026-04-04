package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.MenuRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IMenuRoleRepository extends JpaRepository<MenuRole, Long> {

  /**
   * Find all menu_role entries for a specific menu
   */
  List<MenuRole> findByMenu_IdMenu(Integer menuId);

  /**
   * Find menu_role entry by menu and role
   */
  Optional<MenuRole> findByMenu_IdMenuAndRole_IdRole(Integer menuId, Integer roleId);

  /**
   * Check if a menu_role entry exists
   */
  boolean existsByMenu_IdMenuAndRole_IdRole(Integer menuId, Integer roleId);

  /**
   * Delete all menu_role entries for a specific menu
   */
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM MenuRole mr WHERE mr.menu.idMenu = :menuId")
  void deleteByMenuId(@Param("menuId") Integer menuId);

  /**
   * Delete menu_role entries for a specific menu and role
   */
  @Modifying
  @Query("DELETE FROM MenuRole mr WHERE mr.menu.idMenu = :menuId AND mr.role.idRole = :roleId")
  void deleteByMenuIdAndRoleId(@Param("menuId") Integer menuId, @Param("roleId") Integer roleId);
}

