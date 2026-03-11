package com.tradebot.upstox.llm;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentQuoteContext {

	// Static instrument metadata
	private String instrumentKey;
	private String tradingSymbol;
	private BigDecimal strikePrice;
	private LocalDate expiry;
	private Integer lotSize;
	private BigDecimal tickSize;
	private String segment;
	private String exchange;
	private String name;
	private String assetSymbol;
	private String instrumentType;

	// Raw quote fields
	private BigDecimal lastPrice;
	private BigDecimal averagePrice;
	private BigDecimal openPrice;
	private BigDecimal highPrice;
	private BigDecimal lowPrice;
	private BigDecimal closePrice;
	private long volume;
	private BigDecimal oi;
	private BigDecimal netChange;
	private BigDecimal totalBuyQuantity;
	private BigDecimal totalSellQuantity;

	// Depth / liquidity snapshot
	private BigDecimal bestBid;
	private BigDecimal bestAsk;
	private BigDecimal bestBidQuantity;
	private BigDecimal bestAskQuantity;
	private BigDecimal spreadPercent;
	private BigDecimal topOfBookQuantity;
	private BigDecimal totalDepthQuantity;

	// Derived features for decision confidence
	private BigDecimal vwapDeviation;
	private BigDecimal rangePosition;
	private BigDecimal orderBookImbalance;
	private BigDecimal volumeSurge;
	private BigDecimal gapPercent;
	private BigDecimal priceChangePercent;
	private BigDecimal oiPosition;

	// Optional textual summary that can be constructed before passing to LLM
	private String featureSummary;
}

