package com.tradebot.upstox.service.impl;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.tradebot.upstox.auth.TokenStore;
import com.tradebot.upstox.dto.UserResponse;
import com.tradebot.upstox.dto.UserTokenResponse;
import com.tradebot.upstox.service.AuthenticationService;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

	private static final String AUTHORIZATION_CODE = "authorization_code";

	@Value("${upstox.baseUrl}")
	private String baseUrl;

	@Value("${upstox.apiKey}")
	private String apiKey;

	@Value("${upstox.apiSecret}")
	private String apiSecret;

	@Value("${upstox.redirectUri}")
	private String redirectUri;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private TokenStore tokenStore;

	@Override
	public UserResponse generateAccessToken(String authenticationCode) {
		UserResponse response = new UserResponse();
		if (apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
			response.setResponseMessage("Upstox API key/secret not configured");
			response.setStatusCode(HttpStatus.BAD_REQUEST.value());
			return response;
		}
		String url = baseUrl + "/login/authorization/token";
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("code", authenticationCode);
		body.add("client_id", apiKey);
		body.add("client_secret", apiSecret);
		body.add("redirect_uri", redirectUri);
		body.add("grant_type", AUTHORIZATION_CODE);
		HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
		try {
			ResponseEntity<UserTokenResponse> tokenResponse = restTemplate.exchange(url, HttpMethod.POST, requestEntity, UserTokenResponse.class);
			if (tokenResponse.getStatusCode().equals(HttpStatus.OK) && tokenResponse.getBody() != null) {
				UserTokenResponse tr = tokenResponse.getBody();
				tokenStore.put(tr.getUser_id(), tr.getAccess_token());
				response.setResponseMessage("Access token generated successfully!!");
				response.setStatusCode(HttpStatus.OK.value());
				return response;
			}
			response.setResponseMessage("Failed to get token from Upstox");
			response.setStatusCode(tokenResponse.getStatusCode().value());
		} catch (Exception e) {
			response.setResponseMessage(e.getMessage() != null ? e.getMessage() : "Error while generating access token");
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return response;
	}
}
