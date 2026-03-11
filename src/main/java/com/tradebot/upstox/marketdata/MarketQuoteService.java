package com.tradebot.upstox.marketdata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebot.upstox.auth.TokenStore;
import com.tradebot.upstox.llm.InstrumentQuoteContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketQuoteService {

	private final InstrumentLookupService lookupService;
	private final TokenStore tokenStore;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${upstox.baseUrl}")
	private String baseUrl;

	public Optional<InstrumentQuoteContext> getQuoteContext(String userId,
	                                                        String name,
	                                                        String assetSymbol,
	                                                        LocalDate expiryDate,
	                                                        String instrumentType,
	                                                        BigDecimal strikePrice) {
		Optional<InstrumentMetadata> metadataOpt = lookupService.find(name, assetSymbol, expiryDate, instrumentType, strikePrice);
		if (metadataOpt.isEmpty()) {
			log.warn("No instrument metadata found for {}, {}, {}, {}, {}", name, assetSymbol, expiryDate, instrumentType, strikePrice);
			return Optional.empty();
		}

		InstrumentMetadata metadata = metadataOpt.get();
		Optional<String> tokenOpt = tokenStore.getAccessToken(userId);
		if (tokenOpt.isEmpty()) {
			log.warn("No access token found for user {}, cannot fetch market quote", userId);
			return Optional.empty();
		}

		String instrumentKey = metadata.getInstrumentKey();
		JsonNode quoteNode = getMarketQuoteJson(tokenOpt.get(), instrumentKey);
		if (quoteNode == null) {
			return Optional.empty();
		}

		return Optional.of(buildContextFrom(metadata, quoteNode));
	}

	private JsonNode getMarketQuoteJson(String accessToken, String instrumentKey) {
		try {
			String url = String.format("%s/market-quote/quotes?instrument_key=%s", baseUrl, instrumentKey);
			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", "application/json");
			headers.set("Authorization", "Bearer " + accessToken);
			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				log.warn("Failed to get market quote for {} status={}", instrumentKey, response.getStatusCode());
				return null;
			}
			JsonNode root = objectMapper.readTree(response.getBody());
			JsonNode dataNode = root.path("data");
			if (dataNode.isMissingNode() || !dataNode.fields().hasNext()) {
				return null;
			}
			Map.Entry<String, JsonNode> first = dataNode.fields().next();
			return first.getValue();
		} catch (Exception e) {
			log.error("Error fetching market quote for {}: {}", instrumentKey, e.getMessage());
			return null;
		}
	}

	private InstrumentQuoteContext buildContextFrom(InstrumentMetadata metadata, JsonNode quote) {
		JsonNode ohlc = quote.path("ohlc");
		JsonNode depth = quote.path("depth");
		JsonNode buyDepth = depth.path("buy");
		JsonNode sellDepth = depth.path("sell");

		BigDecimal lastPrice = decimal(quote, "last_price");
		BigDecimal averagePrice = decimal(quote, "average_price");
		BigDecimal open = decimal(ohlc, "open");
		BigDecimal high = decimal(ohlc, "high");
		BigDecimal low = decimal(ohlc, "low");
		BigDecimal close = decimal(ohlc, "close");

		BigDecimal totalBuyQty = decimal(quote, "total_buy_quantity");
		BigDecimal totalSellQty = decimal(quote, "total_sell_quantity");

		BigDecimal bestBid = buyDepth.isArray() && buyDepth.size() > 0 ? decimal(buyDepth.get(0), "price") : null;
		BigDecimal bestAsk = sellDepth.isArray() && sellDepth.size() > 0 ? decimal(sellDepth.get(0), "price") : null;
		BigDecimal bestBidQty = buyDepth.isArray() && buyDepth.size() > 0 ? decimal(buyDepth.get(0), "quantity") : null;
		BigDecimal bestAskQty = sellDepth.isArray() && sellDepth.size() > 0 ? decimal(sellDepth.get(0), "quantity") : null;

		BigDecimal spreadPercent = null;
		if (bestBid != null && bestAsk != null && lastPrice != null && lastPrice.compareTo(BigDecimal.ZERO) > 0) {
			spreadPercent = bestAsk.subtract(bestBid)
					.divide(lastPrice, 6, RoundingMode.HALF_UP);
		}

		BigDecimal topOfBookQty = null;
		if (bestBidQty != null && bestAskQty != null) {
			topOfBookQty = bestBidQty.add(bestAskQty);
		}

		BigDecimal totalDepthQty = BigDecimal.ZERO;
		if (buyDepth.isArray()) {
			for (JsonNode b : buyDepth) {
				totalDepthQty = totalDepthQty.add(decimal(b, "quantity", BigDecimal.ZERO));
			}
		}
		if (sellDepth.isArray()) {
			for (JsonNode s : sellDepth) {
				totalDepthQty = totalDepthQty.add(decimal(s, "quantity", BigDecimal.ZERO));
			}
		}

		BigDecimal vwapDeviation = null;
		if (lastPrice != null && averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) > 0) {
			vwapDeviation = lastPrice.subtract(averagePrice)
					.divide(averagePrice, 6, RoundingMode.HALF_UP);
		}

		BigDecimal rangePosition = null;
		if (high != null && low != null && high.compareTo(low) > 0 && lastPrice != null) {
			rangePosition = lastPrice.subtract(low)
					.divide(high.subtract(low), 6, RoundingMode.HALF_UP);
			if (rangePosition.compareTo(BigDecimal.ZERO) < 0) {
				rangePosition = BigDecimal.ZERO;
			} else if (rangePosition.compareTo(BigDecimal.ONE) > 0) {
				rangePosition = BigDecimal.ONE;
			}
		}

		BigDecimal orderBookImbalance = null;
		if (totalBuyQty != null && totalSellQty != null) {
			BigDecimal denom = totalBuyQty.add(totalSellQty);
			if (denom.compareTo(BigDecimal.ZERO) > 0) {
				orderBookImbalance = totalBuyQty.subtract(totalSellQty)
						.divide(denom, 6, RoundingMode.HALF_UP);
			}
		}

		BigDecimal oi = decimal(quote, "oi");
		BigDecimal oiDayHigh = decimal(quote, "oi_day_high");
		BigDecimal oiDayLow = decimal(quote, "oi_day_low");
		BigDecimal netChange = decimal(quote, "net_change");

		BigDecimal priceChangePercent = null;
		if (lastPrice != null && netChange != null) {
			BigDecimal prevClose = lastPrice.subtract(netChange);
			if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
				priceChangePercent = netChange.divide(prevClose, 6, RoundingMode.HALF_UP);
			}
		}

		BigDecimal oiPosition = null;
		if (oi != null && oiDayHigh != null && oiDayLow != null && oiDayHigh.compareTo(oiDayLow) > 0) {
			oiPosition = oi.subtract(oiDayLow)
					.divide(oiDayHigh.subtract(oiDayLow), 6, RoundingMode.HALF_UP);
		}

		return InstrumentQuoteContext.builder()
				.instrumentKey(metadata.getInstrumentKey())
				.tradingSymbol(metadata.getTradingSymbol())
				.strikePrice(metadata.getStrikePrice())
				.expiry(metadata.getExpiryDate())
				.lotSize(metadata.getLotSize())
				.tickSize(metadata.getTickSize())
				.segment(metadata.getSegment())
				.exchange(metadata.getExchange())
				.name(metadata.getName())
				.assetSymbol(metadata.getAssetSymbol())
				.instrumentType(metadata.getInstrumentType())
				.lastPrice(lastPrice)
				.averagePrice(averagePrice)
				.openPrice(open)
				.highPrice(high)
				.lowPrice(low)
				.closePrice(close)
				.volume(longValue(quote, "volume"))
				.oi(oi)
				.netChange(netChange)
				.totalBuyQuantity(totalBuyQty)
				.totalSellQuantity(totalSellQty)
				.bestBid(bestBid)
				.bestAsk(bestAsk)
				.bestBidQuantity(bestBidQty)
				.bestAskQuantity(bestAskQty)
				.spreadPercent(spreadPercent)
				.topOfBookQuantity(topOfBookQty)
				.totalDepthQuantity(totalDepthQty)
				.vwapDeviation(vwapDeviation)
				.rangePosition(rangePosition)
				.orderBookImbalance(orderBookImbalance)
				.priceChangePercent(priceChangePercent)
				.oiPosition(oiPosition)
				.build();
	}

	private BigDecimal decimal(JsonNode node, String field) {
		return decimal(node, field, null);
	}

	private BigDecimal decimal(JsonNode node, String field, BigDecimal defaultValue) {
		if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
			return defaultValue;
		}
		return node.path(field).decimalValue();
	}

	private long longValue(JsonNode node, String field) {
		if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
			return 0L;
		}
		return node.path(field).asLong();
	}
}

