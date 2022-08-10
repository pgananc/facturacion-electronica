package com.wallparisoft.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wallparisoft.model.Menu;
import com.wallparisoft.repository.IMenuRepository;
import com.wallparisoft.service.IMenuService;

@Service
public class MenuServiceImpl implements IMenuService {

	@Autowired
	private IMenuRepository menuRepository;

	@Override
	public List<Menu> findMenuByUsername(String userName) {		
		List<Menu> menus = new ArrayList<>();
		menuRepository.findMenuByUser(userName).forEach( x -> {
			Menu m = new Menu();
			m.setIdMenu((Integer.parseInt(String.valueOf(x[0]))));
			m.setIcon(String.valueOf(x[1]));
			m.setName(String.valueOf(x[2]));
			m.setUrl(String.valueOf(x[3]));		
	
			menus.add(m);
		});
		return menus;			
	}

}
