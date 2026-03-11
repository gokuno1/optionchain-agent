package com.tradebot.upstox.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InstrumentLookupService {

	private final InstrumentMetadataRepository repository;

	public Optional<InstrumentMetadata> find(String name,
	                                         String assetSymbol,
	                                         LocalDate expiryDate,
	                                         String instrumentType,
	                                         BigDecimal strikePrice) {
		return repository.findByNameAndAssetSymbolAndExpiryDateAndInstrumentTypeAndStrikePrice(
				name,
				assetSymbol,
				expiryDate,
				instrumentType,
				strikePrice
		);
	}
}

