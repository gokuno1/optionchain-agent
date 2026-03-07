package com.tradebot.upstox.llm;

import java.util.List;

import lombok.Data;

@Data
public class OptionChainLLMAnalysis {
	private String direction_bias;
	private String volatility_expectation;
	private List<Double> key_levels;
	private String reasoning;
	private String trade_strike;
	private String confidence;
	private String target_percent;
	private String stop_percent;
}
