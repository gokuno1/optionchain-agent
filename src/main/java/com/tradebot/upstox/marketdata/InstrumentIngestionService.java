package com.tradebot.upstox.marketdata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentIngestionService {

	private final InstrumentMetadataRepository repository;

	public void ingestMonthly(List<InstrumentMetadata> instruments, List<String> assetSymbols) {
		if (instruments == null || instruments.isEmpty()) {
			log.warn("Instrument ingestion called with empty or null instruments list");
			return;
		}

		Set<String> assetFilter = assetSymbols != null && !assetSymbols.isEmpty()
				? new HashSet<>(assetSymbols)
				: null;

		LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

		log.info("Starting instrument ingestion. totalInstruments={}, assetFilter={}",
				instruments.size(), assetFilter);

		List<InstrumentMetadata> toSave = instruments.stream()
				.map(metadata -> {
					if (metadata == null) {
						log.warn("Skipping null instrument entry during ingestion");
						return null;
					}
					if (metadata.getExpiry() == null) {
						log.warn("Skipping instrument without expiry: instrumentKey={}", metadata.getInstrumentKey());
						return null;
					}
					try {
						LocalDate expiryDate = Instant.ofEpochMilli(metadata.getExpiry())
								.atZone(ZoneId.of("Asia/Kolkata"))
								.toLocalDate();
						metadata.setExpiryDate(expiryDate);
						return metadata;
					} catch (Exception e) {
						log.error("Failed to parse expiry for instrumentKey={}", metadata.getInstrumentKey(), e);
						return null;
					}
				})
				.filter(metadata -> metadata != null && !metadata.getExpiryDate().isBefore(today))
				.filter(metadata -> {
					if (assetFilter == null) {
						return true;
					}
					String symbol = metadata.getAssetSymbol() != null
							? metadata.getAssetSymbol()
							: metadata.getName();
					boolean keep = symbol != null && assetFilter.contains(symbol);
					if (!keep) {
						log.debug("Filtering out instrumentKey={} symbol={} by asset filter",
								metadata.getInstrumentKey(), symbol);
					}
					return keep;
				})
				.toList();

		if (toSave.isEmpty()) {
			log.warn("No instruments qualified for ingestion after filtering. totalReceived={}", instruments.size());
			return;
		}

		try {
			repository.saveAll(toSave);
			log.info("Instrument ingestion completed. savedCount={}", toSave.size());
		} catch (Exception e) {
			log.error("Failed to save instruments to MongoDB. attemptedCount={}", toSave.size(), e);
		}
	}
}

