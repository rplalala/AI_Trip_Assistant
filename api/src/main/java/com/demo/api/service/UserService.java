package com.demo.api.service;

import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;

public interface UserService {
    String login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);
}
