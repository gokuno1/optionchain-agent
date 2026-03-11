package com.tradebot.upstox.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface QuoteRefinementAnalyst {

	@SystemMessage("""
			You are an intraday options trading agent. Your job is to refine an existing trade analysis using fresh market quote context for the chosen strike.
			Input:
			- originalAnalysisJson: JSON from the first analysis step (direction_bias, reasoning, trade_strike, instrument_type, strike_price, target_percent, stop_percent, confidence, etc.).
			- quoteContextJson: JSON describing the latest option quote and derived features: VWAP deviation, range position, order book imbalance, volume surge, gap context, momentum alignment (priceChangePercent + oiPosition), and liquidity depth (spread, depth quantities).
			
			Task:
			1) Use quoteContextJson ONLY as an additional signal layer on top of originalAnalysisJson.
			2) If the quote context clearly SUPPORTS the original trade idea (e.g. strong alignment in direction, healthy liquidity, acceptable spreads), you may keep or increase confidence and optionally fine-tune target_percent and stop_percent.
			3) If the quote context clearly CONTRADICTS the original idea (e.g. opposite momentum, very poor liquidity, extreme gap against the trade), you may LOWER confidence or flip to NO_TRADE by setting trade_strike: "NO_TRADE", target_percent: "0", stop_percent: "0".
			4) Always explain briefly in reasoning how quoteContextJson influenced any adjustment (e.g. mention VWAP deviation, range position, order book imbalance, liquidity).
			5) Output a single JSON object, following the same schema as the original analysis:
			   {"direction_bias":"bullish|bearish|neutral","volatility_expectation":"low|moderate|high","key_levels":[number],
			    "trade_strike":"e.g. 24500 CE or NO_TRADE","instrument_type":"CE|PE or empty when NO_TRADE","strike_price":"number or 0 when NO_TRADE",
			    "target_percent":"number","stop_percent":"number","reasoning":"short explanation","confidence":"low|medium|high"}
			""")
	@UserMessage("""
		Underlying index: {index}
		Expiry: {expiry}
		
		Original analysis JSON:
		{originalAnalysisJson}
		
		Quote context JSON:
		{quoteContextJson}
		""")
	OptionChainLLMAnalysis refine(
			@V("index") String index,
			@V("expiry") String expiry,
			@V("originalAnalysisJson") String originalAnalysisJson,
			@V("quoteContextJson") String quoteContextJson
	);
}

