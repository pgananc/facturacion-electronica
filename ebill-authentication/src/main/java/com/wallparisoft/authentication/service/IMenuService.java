package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.entity.Menu;
import java.util.List;

public interface IMenuService {

  List<Menu> findMenuByUsername(String userName);
}
