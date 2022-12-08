package com.wallparisoft.service;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.entity.User;
import com.wallparisoft.response.TokenResponse;

public interface ILoginService {
	
	User validateUserName(String userName, String password) throws Exception;

	TokenResponse authentication(UserDto userDto) throws Exception;
	

}
