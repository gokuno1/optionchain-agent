# Upstox Option Chain (Standalone)

Minimal Spring Boot app: Upstox API connection, option chain fetch, and option chain analysis (rule engine + optional LLM). No database, JPA, or order placement.

## Requirements

- Java 17
- Maven 3.6+

## Configuration

Set in `application.yml` or environment:

- `upstox.apiKey`, `upstox.apiSecret`, `upstox.redirectUri` – for token exchange (or use env `UPSTOX_API_KEY`, `UPSTOX_API_SECRET`, `UPSTOX_REDIRECT_URI`)
- `upstox.userId` – default user for scheduled run
- `upstox.tokenFilePath` – optional path to JSON file for token persistence (default: `tokens.json` in the current working directory). Set `UPSTOX_TOKEN_FILE` to override. Tokens are loaded on startup and saved whenever a new token is received, so you don’t need to re-authenticate after a restart.
- `scheduler.enabled`, `scheduler.index` – optional scheduled option chain run

Optional LLM (Ollama):

- `llm.enabled: true` – enable LLM analysis (requires Ollama at `http://localhost:11434` with model `mistral`)

## Run

```bash
mvn spring-boot:run
```

## API

1. **POST /auth/token?code=...**  
   Exchange Upstox auth code for access token. Token is stored in memory and persisted to a JSON file (see `upstox.tokenFilePath`) by `user_id`. Call this once (e.g. after redirect from Upstox login) before using the option chain. After an app restart, tokens are loaded from the file so you don’t need to call this again until the token expires.

2. **POST /option/chain?index=...&expiryDate=...&userId=...**  
   Fetch option chain for the given index and expiry, run rule-based analysis and (if enabled) LLM analysis. Returns `OptionAnalysisResponse` with averages, ATM greeks, rule signals, and optional LLM analysis.

Example:

```bash
curl -X POST "http://localhost:8080/option/chain?index=Nifty%2050&expiryDate=2025-03-13&userId=BN8600"
```

## Project layout

- `auth` – token store with JSON file persistence (load on startup, save on new token)
- `config` – RestTemplate, OAuth, optional LlmConfig
- `controller` – UserAuthController, OptionChainController
- `dto` – request/response and option chain DTOs
- `llm` – OptionChainAnalyst interface and OptionChainLLMAnalysis DTO
- `model` – OptionChainAnalysisData (POJO, no persistence)
- `service` – AuthenticationService, OptionChainService, OptionChainRuleEngine
