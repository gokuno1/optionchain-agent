package com.tradebot.upstox.dto.optionchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PutOptions {
	private String instrument_key;
	private MarketData market_data;
	private OptionGreeks option_greeks;
}
