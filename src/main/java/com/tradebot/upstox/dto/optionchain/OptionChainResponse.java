package com.tradebot.upstox.dto.optionchain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionChainResponse {
	private String status;
	private List<OptionChainData> data;
}
