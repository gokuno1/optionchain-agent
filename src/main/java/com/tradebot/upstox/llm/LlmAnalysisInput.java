package com.tradebot.upstox.llm;

import com.tradebot.upstox.dto.optionchain.OptionChainRuleSignals;
import com.tradebot.upstox.dto.optionchain.OptionGreeks;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal payload for LLM analysis to reduce token count and improve response time.
 * Only includes fields the analyst needs: rule signals and key metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmAnalysisInput {
	private double spotPrice;
	private double atmStrikePrice;
	private double putCallRatio;
	private double avgCallIv;
	private double avgPutIv;
	private OptionGreeks callGreeks;
	private OptionGreeks putGreeks;
	private OptionChainRuleSignals ruleSignals;
}
