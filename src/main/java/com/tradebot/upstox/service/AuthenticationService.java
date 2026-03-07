package com.tradebot.upstox.service;

import com.tradebot.upstox.dto.UserResponse;

public interface AuthenticationService {
	UserResponse generateAccessToken(String authenticationCode);
}
