package com.tradebot.upstox.dto.optionchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionGreeks {
	private double vega;
	private double theta;
	private double gamma;
	private double delta;
	private double iv;
}
