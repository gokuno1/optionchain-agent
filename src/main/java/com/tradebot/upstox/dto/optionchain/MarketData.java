package com.tradebot.upstox.dto.optionchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketData {
	private double ltp;
	private int volume;
	private double oi;
	private double close_price;
	private double bid_price;
	private int bid_qty;
	private double ask_price;
	private int ask_qty;
	private double prev_oi;
}
