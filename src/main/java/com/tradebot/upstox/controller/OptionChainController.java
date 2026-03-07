package com.tradebot.upstox.controller;

import java.util.Date;

import com.tradebot.upstox.common.DateUtils;
import com.tradebot.upstox.dto.OptionAnalysisResponse;
import com.tradebot.upstox.service.OptionChainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/option")
@Slf4j
public class OptionChainController {

	@Value("${scheduler.enabled:false}")
	private boolean schedulerEnabled;
	@Value("${upstox.userId:}")
	private String userId;
	@Value("${scheduler.index:Nifty 50}")
	private String index;

	@Autowired
	private OptionChainService optionChainService;

	@PostMapping("/chain")
	public ResponseEntity<OptionAnalysisResponse> getOptionChainAnalysis(
			@RequestParam String expiryDate,
			@RequestParam String index,
			@RequestParam String userId) {
		log.info("Option chain request at {}", new Date());
		OptionAnalysisResponse response = optionChainService.getOptionChainAnalysis(index, expiryDate, userId);
		if (response.getStatusCode() == HttpStatus.OK.value()) {
			return ResponseEntity.ok(response);
		}
		return ResponseEntity.status(response.getStatusCode()).body(response);
	}

	@Scheduled(cron = "0 */5 9-15 * * 1-5")
	public void scheduleOptionChainAnalysis() {
		if (schedulerEnabled && userId != null && !userId.isBlank()) {
			String expiryDate = DateUtils.getThursdayDateStringFormat();
			log.info("Scheduled option chain for {} expiry {} at {}", index, expiryDate, new Date());
			optionChainService.getOptionChainAnalysis(index, expiryDate, userId);
		} else {
			log.debug("Scheduler disabled or userId not set");
		}
	}
}
