package com.wallparisoft.service;

import java.util.List;

import com.wallparisoft.entity.Menu;

public interface IMenuService{
	
	List<Menu> findMenuByUsername(String userName);
}
