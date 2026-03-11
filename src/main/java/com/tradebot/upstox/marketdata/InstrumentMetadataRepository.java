package com.tradebot.upstox.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstrumentMetadataRepository extends MongoRepository<InstrumentMetadata, String> {

	Optional<InstrumentMetadata> findByNameAndAssetSymbolAndExpiryDateAndInstrumentTypeAndStrikePrice(
			String name,
			String assetSymbol,
			LocalDate expiryDate,
			String instrumentType,
			BigDecimal strikePrice
	);
}

