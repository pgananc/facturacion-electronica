package com.wallparisoft.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wallparisoft.model.User;
import com.wallparisoft.repository.ILoginRepository;
import com.wallparisoft.service.ILoginService;

@Service
public class LoginServiceImpl implements ILoginService {

	@Autowired
	private ILoginRepository loginRepository;

	@Override
	public User validateUserName(String userName, String password) throws Exception {
		return loginRepository.validateByUserName(userName, password);
	}
}
