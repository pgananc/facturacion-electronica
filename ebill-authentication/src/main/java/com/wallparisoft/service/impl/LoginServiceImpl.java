package com.wallparisoft.service.impl;

import com.wallparisoft.entity.User;
import com.wallparisoft.repository.ILoginRepository;
import com.wallparisoft.service.ILoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements ILoginService {

	@Autowired
	private ILoginRepository loginRepository;

	@Override
	public User validateUserName(String userName, String password) throws Exception {
		return loginRepository.findByUserNameAndPasswordAndStatus(userName, password,true);
	}
}
