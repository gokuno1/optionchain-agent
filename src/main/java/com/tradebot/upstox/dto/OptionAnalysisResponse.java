package com.tradebot.upstox.dto;

import com.tradebot.upstox.dto.optionchain.OptionChainRuleSignals;
import com.tradebot.upstox.dto.optionchain.OptionGreeks;
import com.tradebot.upstox.llm.OptionChainLLMAnalysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OptionAnalysisResponse extends UserResponse {
	private double avgPutDelta;
	private double avgCallDelta;
	private double avgPutIv;
	private double avgCallIv;
	private double avgPutOtmIv;
	private double avgCallOtmIv;
	private double putCallRatio;
	private double spotPrice;
	private double strikePrice;
	private OptionGreeks callGreeks;
	private OptionGreeks putGreeks;
	private double atmStrikePrice;
	private OptionChainRuleSignals ruleSignals;
	private OptionChainLLMAnalysis llmAnalysis;
}
