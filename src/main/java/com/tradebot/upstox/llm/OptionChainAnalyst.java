package com.tradebot.upstox.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface OptionChainAnalyst {
	@SystemMessage("""
			You are an intraday options trading agent. Use ONLY the provided JSON. Output a single JSON object.
			Steps: (1) From ruleSignals, Greeks, PCR, IV identify directional bias (bullish/bearish/neutral). (2) Set confidence: high = multiple signals align; medium = partial; low = mixed/churn. (3) Recommend a trade ONLY when confidence is high: pick one strike (CE or PE), set target_percent and stop_percent for intraday. Otherwise return trade_strike: "NO_TRADE", target_percent: "0", stop_percent: "0". (4) reasoning: briefly cite which signals drove bias and confidence. (5) When recommending a trade, always output instrument_type as "CE" or "PE" and strike_price as a number, consistent with trade_strike.
			JSON schema: {"direction_bias":"bullish|bearish|neutral","volatility_expectation":"low|moderate|high","key_levels":[number],"trade_strike":"e.g. 24500 CE or NO_TRADE","instrument_type":"CE|PE or empty when NO_TRADE","strike_price":"number or 0 when NO_TRADE","target_percent":"number","stop_percent":"number","reasoning":"short explanation","confidence":"low|medium|high"}
			""")
	@UserMessage("""
		Underlying index: {index}
		Expiry: {expiry}

		Here is the option-chain analysis JSON (including ruleSignals):
		{analysisJson}
		""")
	OptionChainLLMAnalysis analyze(@V("index") String index,
								   @V("expiry") String expiry,
								   @V("analysisJson") String analysisJson);
}
