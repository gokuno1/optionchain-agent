package com.tradebot.upstox.controller;

import java.util.Date;

import com.tradebot.upstox.agent.AgentAnalysisService;
import com.tradebot.upstox.common.DateUtils;
import com.tradebot.upstox.dto.OptionAnalysisResponse;
import com.tradebot.upstox.dto.agent.AgentDecision;
import com.tradebot.upstox.service.OptionChainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

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

	@Autowired
	private AgentAnalysisService agentAnalysisService;

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

//	@Scheduled(cron = "0 */5 9-15 * * 1-5")
	public void scheduleOptionChainAnalysis() {
		if (schedulerEnabled && userId != null && !userId.isBlank()) {
			String expiryDate = DateUtils.getThursdayDateStringFormat();
			log.info("Scheduled option chain for {} expiry {} at {}", index, expiryDate, new Date());
			optionChainService.getOptionChainAnalysis(index, expiryDate, userId);
		} else {
			log.debug("Scheduler disabled or userId not set");
		}
	}

	@GetMapping("/decision")
	@Scheduled(cron = "0 */15 9-16 * * 1-5")
	public ResponseEntity<AgentDecision> getDecision() {
		if (schedulerEnabled) {
			String expiryDate = DateUtils.getThursdayDateStringFormat();
			var decisionOpt = agentAnalysisService.inferDecision(index, expiryDate, userId);
			// Either no data or no material change (deltaScore below threshold)
			return decisionOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
		}else {
			log.info("scheduler disabled");
			return ResponseEntity.ok(null);
		}
	}
}
