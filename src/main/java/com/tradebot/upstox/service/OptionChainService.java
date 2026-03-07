package com.tradebot.upstox.service;

import com.tradebot.upstox.dto.OptionAnalysisResponse;

public interface OptionChainService {
	OptionAnalysisResponse getOptionChainAnalysis(String index, String expiryDate, String userId);
}
