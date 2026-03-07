package com.tradebot.upstox.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenEntry {
	private String userId;
	private String accessToken;
}
