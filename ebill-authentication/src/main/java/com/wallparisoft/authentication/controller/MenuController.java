package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;

import com.wallparisoft.authentication.dto.MenuDto;
import com.wallparisoft.authentication.entity.Menu;
import com.wallparisoft.authentication.mapper.MenuMapper;
import com.wallparisoft.authentication.service.IMenuService;
import com.wallparisoft.ebill.utils.log.EventLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

  private final IMenuService service;
  private final MenuMapper menuMapper;

  @GetMapping(value = "/{userName}")
  public ResponseEntity<List<MenuDto>> getMenus(@PathVariable String userName) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    log.info("📥 [MenuController] GET /api/menu/{} - Consultando menús para usuario", userName);

    List<Menu> menus = service.findMenuByUsername(userName);
    List<MenuDto> menuDtos = menuMapper.toDtoList(menus);

    // Log detallado de la respuesta
    log.info(
        "📤 [MenuController] Response para usuario '{}': {} menús raíz devueltos",
        userName,
        menuDtos.size());
    logMenuDtosStructure(menuDtos, 0);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Returned " + menuDtos.size() + " root menus for user: " + userName),
        RESPONSE,
        LEVEL_001);

    return new ResponseEntity<>(menuDtos, HttpStatus.OK);
  }

  /** Método auxiliar para loggear la estructura de MenuDto de forma recursiva */
  private void logMenuDtosStructure(List<MenuDto> menuDtos, int level) {
    String indent = "  ".repeat(level);
    for (MenuDto menu : menuDtos) {
      String featureInfo =
          menu.getFeatureCode() != null
              ? String.format(" [FeatureCode: %s]", menu.getFeatureCode())
              : " [Sin FeatureCode]";
      log.info(
          "{}├─ {} (ID: {}) - URL: '{}'{} - Submenús: {}",
          indent,
          menu.getName(),
          menu.getId(),
          menu.getUrl() != null ? menu.getUrl() : "null",
          featureInfo,
          menu.getSubMenus() != null ? menu.getSubMenus().size() : 0);

      if (menu.getSubMenus() != null && !menu.getSubMenus().isEmpty()) {
        logMenuDtosStructure(menu.getSubMenus(), level + 1);
      }
    }
  }
}
