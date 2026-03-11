package com.tradebot.upstox.llm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.tradebot.upstox.marketdata.MarketQuoteService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketQuoteTool {

	private final MarketQuoteService marketQuoteService;

	@Tool("Get enriched market quote context for a specific option contract identified by name, asset symbol, expiry date, instrument type (CE/PE) and strike price")
	public InstrumentQuoteContext getQuoteContext(String userId,
	                                              String name,
	                                              String assetSymbol,
	                                              String expiry,
	                                              String instrumentType,
	                                              BigDecimal strikePrice) {
		LocalDate expiryDate = LocalDate.parse(expiry, DateTimeFormatter.ISO_LOCAL_DATE);
		Optional<InstrumentQuoteContext> context = marketQuoteService.getQuoteContext(
				userId,
				name,
				assetSymbol,
				expiryDate,
				instrumentType,
				strikePrice
		);
		return context.orElse(null);
	}
}

