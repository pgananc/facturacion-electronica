package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.entity.Menu;
import com.wallparisoft.authentication.repository.IMenuRepository;
import com.wallparisoft.authentication.service.IMenuService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MenuServiceImpl implements IMenuService {

  private final IMenuRepository menuRepository;

  @Override
  public List<Menu> findMenuByUsername(String userName) {
    // Return all active menus without filtering by menu_role
    // Frontend will handle permission filtering (role + plan)
    List<Object[]> rows = menuRepository.findAllActiveMenus();

    Map<Integer, Menu> allMenus = new HashMap<>();
    List<Menu> topLevelMenus = new ArrayList<>();

    for (Object[] row : rows) {
      Integer idMenu = (Integer) row[0];
      Integer parentId = row[1] != null ? Integer.parseInt(String.valueOf(row[1])) : null;
      String icon = String.valueOf(row[2]);
      String name = String.valueOf(row[3]);
      String url = String.valueOf(row[4]);
      Boolean status = (Boolean) row[5];
      String featureCode = row.length > 6 && row[6] != null ? String.valueOf(row[6]) : null;
      Integer menuOrder = row.length > 7 && row[7] != null ? (Integer) row[7] : null;

      Menu menu = new Menu();
      menu.setIdMenu(idMenu);
      menu.setIcon(icon);
      menu.setName(name);
      menu.setUrl(url);
      menu.setStatus(status);
      menu.setFeatureCode(featureCode);
      menu.setOrder(menuOrder);
      menu.setSubMenus(new ArrayList<>()); // aseguramos estructura

      allMenus.put(idMenu, menu);

      if (parentId == null) {
        topLevelMenus.add(menu);
      } else {
        Menu parent = allMenus.get(parentId);
        // Si el padre aún no fue procesado, lo resolvemos después
        Objects.requireNonNullElseGet(
                parent,
                () ->
                    allMenus.computeIfAbsent(
                        parentId,
                        k -> {
                          Menu m = new Menu();
                          m.setIdMenu(parentId);
                          m.setSubMenus(new ArrayList<>());
                          return m;
                        }))
            .getSubMenus()
            .add(menu);
      }
    }

    // Ordenar menús por menu_order después de construir la jerarquía
    sortMenusByOrder(topLevelMenus);

    return topLevelMenus;
  }

  /**
   * Ordena los menús recursivamente por el campo order (menu_order) Los menús sin order (NULL) se
   * colocan al final
   */
  private void sortMenusByOrder(List<Menu> menus) {
    if (menus == null || menus.isEmpty()) {
      return;
    }
    menus.sort(
        (m1, m2) -> {
          Integer order1 = m1.getOrder() != null ? m1.getOrder() : Integer.MAX_VALUE;
          Integer order2 = m2.getOrder() != null ? m2.getOrder() : Integer.MAX_VALUE;
          // Si tienen el mismo order, ordenar por id_menu como fallback
          if (order1.equals(order2)) {
            return m1.getIdMenu().compareTo(m2.getIdMenu());
          }
          return order1.compareTo(order2);
        });
    // Ordenar recursivamente los submenús
    menus.forEach(menu -> sortMenusByOrder(menu.getSubMenus()));
  }
}
