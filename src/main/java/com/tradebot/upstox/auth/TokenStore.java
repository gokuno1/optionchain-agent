package com.tradebot.upstox.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TokenStore {

	private final ConcurrentHashMap<String, TokenEntry> store = new ConcurrentHashMap<>();

	@Value("${upstox.tokenFilePath:}")
	private String tokenFilePath;

	@Autowired
	private ObjectMapper objectMapper;

	private Path resolvedPath;

	@PostConstruct
	public void init() {
		if (tokenFilePath != null && !tokenFilePath.isBlank()) {
			resolvedPath = Paths.get(tokenFilePath);
		} else {
			String currentDir = System.getProperty("user.dir");
			resolvedPath = Paths.get(currentDir, "tokens.json");
		}
		loadFromFile();
	}

	public void put(String userId, String accessToken) {
		store.put(userId, new TokenEntry(userId, accessToken));
		saveToFile();
	}

	public Optional<TokenEntry> get(String userId) {
		return Optional.ofNullable(store.get(userId));
	}

	public Optional<String> getAccessToken(String userId) {
		return get(userId).map(TokenEntry::getAccessToken);
	}

	private void loadFromFile() {
		if (!Files.exists(resolvedPath)) {
			log.debug("Token file not found at {}, starting with empty store", resolvedPath);
			return;
		}
		try {
			byte[] bytes = Files.readAllBytes(resolvedPath);
			List<TokenEntry> entries = objectMapper.readValue(bytes, new TypeReference<List<TokenEntry>>() {});
			if (entries != null) {
				for (TokenEntry e : entries) {
					if (e != null && e.getUserId() != null) {
						store.put(e.getUserId(), e);
					}
				}
				log.info("Loaded {} token(s) from {}", store.size(), resolvedPath);
			}
		} catch (IOException e) {
			log.warn("Could not load token file from {}: {}", resolvedPath, e.getMessage());
		}
	}

	private synchronized void saveToFile() {
		try {
			Path parent = resolvedPath.getParent();
			if (parent != null && !Files.exists(parent)) {
				Files.createDirectories(parent);
			}
			List<TokenEntry> entries = store.values().stream().collect(Collectors.toList());
			byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(entries);
			Files.write(resolvedPath, bytes);
			log.debug("Saved {} token(s) to {}", store.size(), resolvedPath);
		} catch (IOException e) {
			log.error("Could not save token file to {}: {}", resolvedPath, e.getMessage());
		}
	}
}
