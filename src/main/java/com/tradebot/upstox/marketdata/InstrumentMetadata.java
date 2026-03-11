package com.tradebot.upstox.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "instrument_metadata")
public class InstrumentMetadata {

	// Core JSON fields
	private boolean weekly;
	private String segment;
	private String name;
	private String exchange;

	/**
	 * Expiry as epoch millis, as provided by upstream JSON.
	 */
	private Long expiry;

	@JsonProperty("instrument_type")
	@Field("instrument_type")
	private String instrumentType;

	@JsonProperty("underlying_symbol")
	@Field("underlying_symbol")
	private String underlyingSymbol;

	/**
	 * Logical asset symbol used in lookups, e.g. NIFTY, BANKNIFTY.
	 */
	@JsonProperty("asset_symbol")
	@Field("asset_symbol")
	private String assetSymbol;

	@JsonProperty("instrument_key")
	@Field("instrument_key")
	private String instrumentKey;

	@JsonProperty("lot_size")
	@Field("lot_size")
	private Integer lotSize;

	@JsonProperty("freeze_quantity")
	@Field("freeze_quantity")
	private Double freezeQuantity;

	@JsonProperty("exchange_token")
	@Field("exchange_token")
	private String exchangeToken;

	@JsonProperty("minimum_lot")
	@Field("minimum_lot")
	private Integer minimumLot;

	@JsonProperty("underlying_key")
	@Field("underlying_key")
	private String underlyingKey;

	@JsonProperty("tick_size")
	@Field("tick_size")
	private BigDecimal tickSize;

	@JsonProperty("underlying_type")
	@Field("underlying_type")
	private String underlyingType;

	@JsonProperty("trading_symbol")
	@Field("trading_symbol")
	private String tradingSymbol;

	@JsonProperty("strike_price")
	@Field("strike_price")
	private BigDecimal strikePrice;

	/**
	 * Helper field for querying by date (derived from expiry).
	 */
	@Field("expiryDate")
	private LocalDate expiryDate;
}

