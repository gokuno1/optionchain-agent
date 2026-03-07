package com.tradebot.upstox.dto;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTokenResponse {
	private String email;
	private ArrayList<String> exchanges;
	private ArrayList<String> products;
	private String broker;
	private String user_id;
	private String user_name;
	private ArrayList<String> order_types;
	private String user_type;
	private boolean poa;
	private boolean is_active;
	private String access_token;
	private String extended_token;
}
