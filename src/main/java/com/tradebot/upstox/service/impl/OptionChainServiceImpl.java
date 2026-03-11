package com.tradebot.upstox.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradebot.upstox.auth.TokenStore;
import com.tradebot.upstox.common.VolumeOiKeysEnum;
import com.tradebot.upstox.dto.OptionAnalysisResponse;
import com.tradebot.upstox.dto.optionchain.OptionChainData;
import com.tradebot.upstox.dto.optionchain.OptionChainResponse;
import com.tradebot.upstox.dto.optionchain.OptionChainRuleSignals;
import com.tradebot.upstox.dto.optionchain.OptionGreeks;
import com.tradebot.upstox.llm.*;
import com.tradebot.upstox.model.OptionChainAnalysisData;
import com.tradebot.upstox.service.OptionChainService;
import com.upstox.ApiException;
import com.upstox.auth.OAuth;
import com.upstox.api.GetMarketQuoteLastTradedPriceResponse;
import com.upstox.api.MarketQuoteSymbolLtp;
import io.swagger.client.api.MarketQuoteApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class OptionChainServiceImpl implements OptionChainService {

	@Autowired
	private TokenStore tokenStore;
	@Autowired
	private OAuth getApiDefaultClient;
	@Value("${upstox.baseUrl}")
	private String baseUrl;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private OptionChainAnalyst optionChainAnalyst;
	@Autowired
	private QuoteRefinementAnalyst quoteRefinementAnalyst;
	@Autowired
	private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
	@Autowired(required = false)
	private MarketQuoteTool marketQuoteTool;

	private static final double RISK_FREE_RATE = 0.1;

	@Override
	public OptionAnalysisResponse getOptionChainAnalysis(String index, String expiryDate, String userId) {
		OptionAnalysisResponse finalResponse = new OptionAnalysisResponse();
		Optional<String> accessTokenOpt = tokenStore.getAccessToken(userId);
		if (accessTokenOpt.isEmpty()) {
			finalResponse.setResponseMessage("No access token found for user. Call POST /auth/token first.");
			finalResponse.setStatusCode(HttpStatus.FORBIDDEN.value());
			return finalResponse;
		}
		String accessToken = accessTokenOpt.get();
		ResponseEntity<OptionChainResponse> response = getOptionChainResponseEntity(index, expiryDate, accessToken);
		if (response.getBody() == null || !HttpStatus.OK.equals(response.getStatusCode()) || !"success".equals(response.getBody().getStatus())) {
			finalResponse.setResponseMessage("Failed to get option chain data from Upstox");
			finalResponse.setStatusCode(response.getStatusCode() != null ? response.getStatusCode().value() : HttpStatus.NO_CONTENT.value());
			return finalResponse;
		}
		List<OptionChainData> optionChainData = response.getBody().getData();
		if (optionChainData == null || optionChainData.isEmpty()) {
			finalResponse.setResponseMessage("Empty option chain data received from Upstox");
			finalResponse.setStatusCode(HttpStatus.NO_CONTENT.value());
			return finalResponse;
		}
		log.info("Option chain retrieved successfully");
		double spotPrice = optionChainData.get(0).getUnderlying_spot_price();
		List<Double> strikePrices = calculateStrikePrices(spotPrice, 50, 15);
		List<OptionChainData> filteredList = getFilteredOptionChainData(strikePrices, optionChainData);
		OptionChainAnalysisData analysisData = calculateAverages(filteredList, spotPrice, accessToken);
		OptionChainRuleEngine ruleEngine = new OptionChainRuleEngine();
		OptionChainRuleSignals ruleSignals = ruleEngine.evaluate(filteredList, spotPrice);

		finalResponse.setResponseMessage("Analysis completed successfully");
		finalResponse.setStatusCode(HttpStatus.OK.value());
		finalResponse.setAvgCallDelta(analysisData.getAvgCallDelta());
		finalResponse.setAvgPutDelta(analysisData.getAvgPutDelta());
		finalResponse.setAvgCallIv(analysisData.getAvgCallIv());
		finalResponse.setAvgPutIv(analysisData.getAvgPutIv());
		finalResponse.setAvgCallOtmIv(analysisData.getAvgCallOtmIv());
		finalResponse.setAvgPutOtmIv(analysisData.getAvgPutOtmIv());
		finalResponse.setPutCallRatio(analysisData.getPutCallRatio());
		finalResponse.setStrikePrice(analysisData.getStrikePrice());
		finalResponse.setSpotPrice(spotPrice);
		finalResponse.setRuleSignals(ruleSignals);

		double atmStrike = analysisData.getStrikePrice();
		Optional<OptionChainData> atmRow = filteredList.stream()
				.filter(option -> option.getStrike_price() == atmStrike)
				.findFirst();
		if (atmRow.isPresent()) {
			finalResponse.setCallGreeks(atmRow.get().getCall_options().getOption_greeks());
			finalResponse.setPutGreeks(atmRow.get().getPut_options().getOption_greeks());
		}
		finalResponse.setAtmStrikePrice(atmStrike);

		if (optionChainAnalyst != null) {
			try {
				LlmAnalysisInput llmInput = LlmAnalysisInput.builder()
						.spotPrice(finalResponse.getSpotPrice())
						.atmStrikePrice(finalResponse.getAtmStrikePrice())
						.putCallRatio(finalResponse.getPutCallRatio())
						.avgCallIv(finalResponse.getAvgCallIv())
						.avgPutIv(finalResponse.getAvgPutIv())
						.callGreeks(finalResponse.getCallGreeks())
						.putGreeks(finalResponse.getPutGreeks())
						.ruleSignals(finalResponse.getRuleSignals())
						.build();
				String analysisJson = objectMapper.writeValueAsString(llmInput);
				OptionChainLLMAnalysis llmAnalysis = optionChainAnalyst.analyze(index, expiryDate, analysisJson);
				log.info("llm analysis completed");

				OptionChainLLMAnalysis finalAnalysis = llmAnalysis;

				if (marketQuoteTool != null
						&& llmAnalysis != null
						&& llmAnalysis.getTrade_strike() != null
						&& !"NO_TRADE".equalsIgnoreCase(llmAnalysis.getTrade_strike())
						&& llmAnalysis.getConfidence() != null
						&& ("medium".equalsIgnoreCase(llmAnalysis.getConfidence())
						|| "high".equalsIgnoreCase(llmAnalysis.getConfidence()))
						&& llmAnalysis.getInstrumentType() != null
						&& llmAnalysis.getStrikePrice() != null) {
					try {
						String indexName = index.equalsIgnoreCase("nifty 50") ? "NIFTY" : index;
						String instrumentExpiryDate = resolveNextTuesdayOrMondayExpiry(accessToken);
						InstrumentQuoteContext quoteContext = marketQuoteTool.getQuoteContext(
								userId,
								indexName,
								indexName,
								instrumentExpiryDate,
								llmAnalysis.getInstrumentType(),
								BigDecimal.valueOf(llmAnalysis.getStrikePrice())
						);
						log.info("Market quote context fetched for trade strike {}", llmAnalysis.getTrade_strike());

						if (quoteRefinementAnalyst != null && quoteContext != null) {
							String originalAnalysisJson = objectMapper.writeValueAsString(llmAnalysis);
							String quoteContextJson = objectMapper.writeValueAsString(quoteContext);
							finalAnalysis = quoteRefinementAnalyst.refine(indexName, instrumentExpiryDate, originalAnalysisJson, quoteContextJson);
							log.info("llm quote-based refinement completed");
						}
					} catch (Exception ex) {
						log.error("Failed to refine trade using market quote for trade strike {}: {}",
								llmAnalysis.getTrade_strike(), ex.getMessage());
					}
				}

				finalResponse.setLlmAnalysis(finalAnalysis);
			} catch (Exception e) {
				log.error("Failed to invoke LLM analysis: {}", e.getMessage());
			}
		}
		return finalResponse;
	}

	private String resolveNextTuesdayOrMondayExpiry(String accessToken) {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
		LocalDate upcomingTuesday;
		if (today.getDayOfWeek().getValue() <= DayOfWeek.TUESDAY.getValue()) {
			upcomingTuesday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
		} else {
			upcomingTuesday = today.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.TUESDAY));
		}

		// If Tuesday is a market holiday, use Monday instead.
		LocalDate expiryDate = isMarketHoliday(upcomingTuesday, accessToken) ? upcomingTuesday.minusDays(1) : upcomingTuesday;
		return expiryDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	private boolean isMarketHoliday(LocalDate date, String accessToken) {
		try {
			String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
			String url = String.format("%s/market/holidays/%s", baseUrl, formattedDate);

			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", "application/json");
			headers.set("Content-Type", "application/json");
			headers.set("Authorization", "Bearer " + accessToken);

			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				log.warn("Holiday API call failed for date {} with status {}", formattedDate, response.getStatusCode());
				return false;
			}

			JsonNode root = objectMapper.readTree(response.getBody());
			String status = root.path("status").asText("");
			JsonNode dataNode = root.path("data");

			boolean isHoliday = "success".equalsIgnoreCase(status)
					&& dataNode.isArray()
					&& dataNode.size() > 0;

			log.info("Holiday check for date {} -> isHoliday={}", formattedDate, isHoliday);
			return isHoliday;
		} catch (Exception e) {
			log.error("Failed to check market holiday for date {}: {}", date, e.getMessage());
			return false;
		}
	}

	private ResponseEntity<OptionChainResponse> getOptionChainResponseEntity(String index, String expiryDate, String accessToken) {
		String instrument = "NSE_INDEX|" + index;
		String url = String.format(baseUrl + "/option/chain?instrument_key=%s&expiry_date=%s", instrument, expiryDate);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", "application/json");
		headers.set("Authorization", "Bearer " + accessToken);
		HttpEntity<String> entity = new HttpEntity<>(headers);
		return restTemplate.exchange(url, HttpMethod.GET, entity, OptionChainResponse.class);
	}

	private List<Double> calculateStrikePrices(double spotPrice, int step, int count) {
		List<Double> strikePrices = new ArrayList<>();
		double baseStrikePrice = Math.floor(spotPrice / step) * step;
		for (int i = count; i > 0; i--) {
			strikePrices.add(baseStrikePrice - i * step);
		}
		strikePrices.add(baseStrikePrice);
		for (int i = 1; i <= count; i++) {
			strikePrices.add(baseStrikePrice + i * step);
		}
		Collections.sort(strikePrices);
		return strikePrices;
	}

	private List<OptionChainData> getFilteredOptionChainData(List<Double> strikePrices, List<OptionChainData> optionChainData) {
		return optionChainData.stream()
				.filter(option -> strikePrices.contains(option.getStrike_price()))
				.collect(Collectors.toList());
	}

	private OptionChainAnalysisData calculateAverages(List<OptionChainData> options, double spotPrice, String authToken) {
		ExecutorService executor = Executors.newFixedThreadPool(9);
		CompletableFuture<Double> averagePutIvFuture = CompletableFuture.supplyAsync(() ->
				options.stream().mapToDouble(o -> o.getPut_options().getOption_greeks().getIv()).average().orElse(0.0), executor);
		CompletableFuture<Double> averageCallIvFuture = CompletableFuture.supplyAsync(() ->
				options.stream().mapToDouble(o -> o.getCall_options().getOption_greeks().getIv()).average().orElse(0.0), executor);
		CompletableFuture<Double> averagePutDeltaFuture = CompletableFuture.supplyAsync(() ->
				options.stream().mapToDouble(o -> o.getPut_options().getOption_greeks().getDelta()).average().orElse(0.0), executor);
		CompletableFuture<Double> averageCallDeltaFuture = CompletableFuture.supplyAsync(() ->
				options.stream().mapToDouble(o -> o.getCall_options().getOption_greeks().getDelta()).average().orElse(0.0), executor);
		CompletableFuture<Double> averageCallOtmIv = CompletableFuture.supplyAsync(() ->
				options.stream().filter(o -> o.getStrike_price() > spotPrice).mapToDouble(o -> o.getCall_options().getOption_greeks().getIv()).average().orElse(0.0), executor);
		CompletableFuture<Double> averagePutOtmIv = CompletableFuture.supplyAsync(() ->
				options.stream().filter(o -> o.getStrike_price() < spotPrice).mapToDouble(o -> o.getPut_options().getOption_greeks().getIv()).average().orElse(0.0), executor);
		CompletableFuture<Double> putCallRatioFuture = CompletableFuture.supplyAsync(() -> calculatePCR(options), executor);
		CompletableFuture<Double> vixLtp = CompletableFuture.supplyAsync(() -> getLtpVix(authToken), executor);
		CompletableFuture<String> putCallParityFuture = CompletableFuture.supplyAsync(() -> calculatePutCallParity(options), executor);
		CompletableFuture<Map<String, Long>> volumeOiFuture = CompletableFuture.supplyAsync(() -> calculateVolumeAndOI(options), executor);
		CompletableFuture<Double> averageCallThetaFuture = CompletableFuture.supplyAsync(() ->
				options.stream().mapToDouble(o -> o.getCall_options().getOption_greeks().getTheta()).average().orElse(0.0), executor);
		CompletableFuture<Double> averagePutThetaFuture = CompletableFuture.supplyAsync(() ->
				options.stream().mapToDouble(o -> o.getPut_options().getOption_greeks().getTheta()).average().orElse(0.0), executor);

		OptionChainAnalysisData averages = new OptionChainAnalysisData();
		try {
			CompletableFuture.allOf(averagePutIvFuture, averageCallIvFuture, averagePutDeltaFuture, averageCallDeltaFuture,
					averageCallOtmIv, averagePutOtmIv, putCallRatioFuture, vixLtp, putCallParityFuture, volumeOiFuture,
					averageCallThetaFuture, averagePutThetaFuture).join();
			Double vixVal = vixLtp.get();
			averages.setAvgPutIv(averagePutIvFuture.get());
			averages.setAvgCallIv(averageCallIvFuture.get());
			averages.setAvgPutDelta(averagePutDeltaFuture.get());
			averages.setAvgCallDelta(averageCallDeltaFuture.get());
			averages.setAvgCallOtmIv(averageCallOtmIv.get());
			averages.setAvgPutOtmIv(averagePutOtmIv.get());
			averages.setPutCallRatio(putCallRatioFuture.get());
			averages.setVixLtp(vixVal != null ? vixVal : 0.0);
			averages.setCreatedDate(new Date());
			averages.setSpotPrice(spotPrice);
			averages.setStrikePrice(findClosestATMStrikePrice(spotPrice));
			averages.setPutCallParity(putCallParityFuture.get());
			Map<String, Long> volOi = volumeOiFuture.get();
			averages.setCallVolume(BigInteger.valueOf(volOi.getOrDefault(VolumeOiKeysEnum.CALL_VOLUME.name(), 0L)));
			averages.setPutVolume(BigInteger.valueOf(volOi.getOrDefault(VolumeOiKeysEnum.PUT_VOLUME.name(), 0L)));
			averages.setCallTotalOI(BigInteger.valueOf(volOi.getOrDefault(VolumeOiKeysEnum.CALL_TOTALOI.name(), 0L)));
			averages.setPutTotalOI(BigInteger.valueOf(volOi.getOrDefault(VolumeOiKeysEnum.PUT_TOTALOI.name(), 0L)));
			averages.setCallChangeOI(BigInteger.valueOf(volOi.getOrDefault(VolumeOiKeysEnum.CALL_CHANGEOI.name(), 0L)));
			averages.setPutChangeOI(BigInteger.valueOf(volOi.getOrDefault(VolumeOiKeysEnum.PUT_CHANGEOI.name(), 0L)));
			averages.setAvgCallTheta(averageCallThetaFuture.get());
			averages.setAvgPutTheta(averagePutThetaFuture.get());
		} catch (Exception e) {
			log.error("Error while processing averages {}", e.getMessage());
		} finally {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
			}
		}
		return averages;
	}

	private Double getLtpVix(String authToken) {
		getApiDefaultClient.setAccessToken(authToken);
		MarketQuoteApi apiInstance = new MarketQuoteApi();
		String symbol = "NSE_INDEX|India VIX";
		String apiVersion = "v2";
		try {
			GetMarketQuoteLastTradedPriceResponse result = apiInstance.ltp(symbol, apiVersion);
			if (result.getData() == null || result.getData().isEmpty()) {
				log.error("Empty VIX LTP response");
				return null;
			}
			MarketQuoteSymbolLtp vixLtp = result.getData().get("NSE_INDEX:India VIX");
			return vixLtp != null ? vixLtp.getLastPrice() : null;
		} catch (ApiException e) {
			log.error("Exception getting VIX LTP: {}", e.getMessage());
			return null;
		}
	}

	private double findClosestATMStrikePrice(double spotPrice) {
		int rounded = (int) Math.round(spotPrice / 50.0);
		return rounded * 50.0;
	}

	private double calculatePCR(List<OptionChainData> options) {
		double callOI = options.stream().mapToDouble(o -> o.getCall_options().getMarket_data().getOi()).sum();
		double putOI = options.stream().mapToDouble(o -> o.getPut_options().getMarket_data().getOi()).sum();
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.CEILING);
		return Double.parseDouble(df.format(putOI / callOI));
	}

	private String calculatePutCallParity(List<OptionChainData> options) {
		int callCount = 0;
		int putCount = 0;
		long timeToExpiration = calculateBusinessDays(options.get(0).getExpiry());
		for (OptionChainData option : options) {
			double pvStrikePrice = option.getStrike_price() * Math.exp(-RISK_FREE_RATE * timeToExpiration);
			double callSide = option.getCall_options().getMarket_data().getLtp() + pvStrikePrice;
			double putSide = option.getPut_options().getMarket_data().getLtp() + option.getUnderlying_spot_price();
			if (callSide > putSide) callCount++;
			else if (callSide < putSide) putCount++;
		}
		return callCount > putCount ? "Call" : "Put";
	}

	private long calculateBusinessDays(String inputDateStr) {
		LocalDate inputDate = LocalDate.parse(inputDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
		LocalDate currentDate = LocalDate.now();
		long daysBetween = ChronoUnit.DAYS.between(currentDate, inputDate);
		long businessDays = 0;
		LocalDate date = currentDate;
		for (long i = 0; i <= daysBetween; i++) {
			if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
				businessDays++;
			}
			date = date.plusDays(1);
		}
		return businessDays;
	}

	private Map<String, Long> calculateVolumeAndOI(List<OptionChainData> options) {
		Map<String, Long> volumeAndOiMap = new HashMap<>();
		double callTotalOI = options.stream().mapToDouble(o -> o.getCall_options().getMarket_data().getOi()).sum();
		volumeAndOiMap.put(VolumeOiKeysEnum.CALL_TOTALOI.name(), (long) callTotalOI);
		double putTotalOI = options.stream().mapToDouble(o -> o.getPut_options().getMarket_data().getOi()).sum();
		volumeAndOiMap.put(VolumeOiKeysEnum.PUT_TOTALOI.name(), (long) putTotalOI);
		double callChangeOI = options.stream().mapToDouble(o -> o.getCall_options().getMarket_data().getOi() - o.getCall_options().getMarket_data().getPrev_oi()).sum();
		volumeAndOiMap.put(VolumeOiKeysEnum.CALL_CHANGEOI.name(), (long) callChangeOI);
		double putChangeOI = options.stream().mapToDouble(o -> o.getPut_options().getMarket_data().getOi() - o.getPut_options().getMarket_data().getPrev_oi()).sum();
		volumeAndOiMap.put(VolumeOiKeysEnum.PUT_CHANGEOI.name(), (long) putChangeOI);
		double callVolume = options.stream().mapToDouble(o -> o.getCall_options().getMarket_data().getVolume()).sum();
		volumeAndOiMap.put(VolumeOiKeysEnum.CALL_VOLUME.name(), (long) callVolume);
		double putVolume = options.stream().mapToDouble(o -> o.getPut_options().getMarket_data().getVolume()).sum();
		volumeAndOiMap.put(VolumeOiKeysEnum.PUT_VOLUME.name(), (long) putVolume);
		return volumeAndOiMap;
	}
}
