package com.wallparisoft.service;

import com.wallparisoft.entity.User;

public interface ILoginService {
	
	User validateUserName(String userName, String password) throws Exception;
	

}
