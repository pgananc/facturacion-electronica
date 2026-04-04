package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.dto.MenuRequestDto;
import com.wallparisoft.authentication.dto.MenuResponseDto;
import java.util.List;

public interface IMenuAdminService {

  /**
   * Lista todos los menús en estructura de árbol completo
   *
   * @return Lista de menús raíz con sus submenús
   */
  List<MenuResponseDto> getAllMenus();

  /**
   * Obtiene un menú específico por ID
   *
   * @param id ID del menú
   * @return MenuResponseDto con la información del menú
   */
  MenuResponseDto getMenuById(Integer id);

  /**
   * Crea un nuevo menú
   *
   * @param request Datos del menú a crear
   * @return MenuResponseDto con el menú creado
   */
  MenuResponseDto createMenu(MenuRequestDto request);

  /**
   * Actualiza un menú existente
   *
   * @param id ID del menú a actualizar
   * @param request Datos actualizados del menú
   * @return MenuResponseDto con el menú actualizado
   */
  MenuResponseDto updateMenu(Integer id, MenuRequestDto request);

  /**
   * Elimina físicamente un menú y todos sus registros relacionados en cascada.
   * Elimina:
   * - Todos los registros en menu_role asociados al menú
   * - El feature_permission si solo este menú lo está usando
   * - El menú mismo
   * - Todos los submenús recursivamente
   *
   * @param id ID del menú a eliminar físicamente
   */
  void hardDeleteMenu(Integer id);

  /**
   * Valida si un usuario tiene rol super_admin
   *
   * @param userName Nombre de usuario
   * @return true si el usuario es super_admin, false en caso contrario
   */
  boolean isSuperAdmin(String userName);
}
