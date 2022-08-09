package com.wallparisoft.service;

import java.util.List;

import com.wallparisoft.model.Menu;

public interface IMenuService{
	
	List<Menu> findMenuByUsername(String userName);
}
