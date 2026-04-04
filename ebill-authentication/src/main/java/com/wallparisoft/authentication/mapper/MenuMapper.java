package com.wallparisoft.authentication.mapper;

import com.wallparisoft.authentication.dto.MenuDto;
import com.wallparisoft.authentication.entity.Menu;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MenuMapper {

  @Mapping(source = "idMenu", target = "id")
  @Mapping(source = "featureCode", target = "featureCode")
  @Mapping(source = "subMenus", target = "subMenus")
  @Mapping(source = "parent.idMenu", target = "parentId")
  @Mapping(source = "status", target = "active")
  @Mapping(source = "order", target = "order")
  MenuDto toDto(Menu menu);

  List<MenuDto> toDtoList(List<Menu> menus);
}
