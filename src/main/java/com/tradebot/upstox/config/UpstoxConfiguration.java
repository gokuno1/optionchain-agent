package com.tradebot.upstox.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.upstox.ApiClient;
import com.upstox.auth.OAuth;

@Configuration
public class UpstoxConfiguration {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	static OAuth getApiDefaultClient() {
		ApiClient defaultClient = com.upstox.Configuration.getDefaultApiClient();
		OAuth oauth2 = (OAuth) defaultClient.getAuthentication("OAUTH2");
		return oauth2;
	}
}
