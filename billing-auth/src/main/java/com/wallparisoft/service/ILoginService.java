package com.wallparisoft.service;

import com.wallparisoft.model.User;

public interface ILoginService {
	
	User validateUserName(String userName, String password) throws Exception;
	

}
