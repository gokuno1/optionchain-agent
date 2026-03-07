package com.tradebot.upstox.dto.optionchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionChainData {
	private String expiry;
	private double pcr;
	private double strike_price;
	private String underlying_key;
	private double underlying_spot_price;
	private CallOptions call_options;
	private PutOptions put_options;
}
