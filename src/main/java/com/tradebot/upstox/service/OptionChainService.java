package com.tradebot.upstox.service;

import com.tradebot.upstox.dto.OptionAnalysisResponse;
import com.tradebot.upstox.dto.agent.AgentOptionChainInput;

import java.util.Optional;

public interface OptionChainService {

	OptionAnalysisResponse getOptionChainAnalysis(String index, String expiryDate, String userId);

	/**
	 * Returns filtered option chain, spot, VIX and expiry for the agent pipeline.
	 * Empty if token missing or chain fetch fails.
	 */
	Optional<AgentOptionChainInput> getOptionChainDataForAgent(String index, String expiryDate, String userId);
}
