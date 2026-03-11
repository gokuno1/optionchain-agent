package com.tradebot.upstox.marketdata;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
@Slf4j
public class InstrumentIngestionController {

	private final InstrumentIngestionService ingestionService;

	@PostMapping(value = "/monthly-ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> ingestMonthly(@RequestBody List<InstrumentMetadata> instruments,
	                                            @RequestParam(value = "assets", required = false) List<String> assets) {
		try {
			ingestionService.ingestMonthly(instruments, assets);
			return ResponseEntity.ok("Ingestion completed");
		} catch (Exception e) {
			log.error("Failed to ingest instruments", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to ingest instruments");
		}
	}
}

