package com.tradebot.upstox.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tradebot.upstox.dto.UserResponse;
import com.tradebot.upstox.service.AuthenticationService;

@RestController
@RequestMapping("/auth")
public class UserAuthController {

	@Autowired
	private AuthenticationService authenticationService;

	@PostMapping("/token")
	public ResponseEntity<UserResponse> generateToken(@RequestHeader("authCode") String code) {
		UserResponse response = authenticationService.generateAccessToken(code);
		if (response == null) {
			response = new UserResponse();
			response.setResponseMessage("Error while processing the request");
			response.setStatusCode(500);
		}
		return ResponseEntity.status(response.getStatusCode()).body(response);
	}
}
