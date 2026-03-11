package com.tradebot.upstox.config;

import java.time.Duration;

import com.tradebot.upstox.llm.OptionChainAnalyst;
import com.tradebot.upstox.llm.QuoteRefinementAnalyst;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true")
public class LlmConfig {

	@Value("${llm.timeout-seconds:90}")
	private int timeoutSeconds;

	@Bean
	public ChatLanguageModel ollamaMistralModel() {
		return OllamaChatModel.builder()
				.baseUrl("http://localhost:11434")
				.modelName("mistral")
				.temperature(0.2)
				.timeout(Duration.ofSeconds(timeoutSeconds))
				.build();
	}

	@Bean
	public OptionChainAnalyst optionChainAnalyst(ChatLanguageModel ollamaMistralModel) {
		return AiServices.create(OptionChainAnalyst.class, ollamaMistralModel);
	}

	@Bean
	public QuoteRefinementAnalyst quoteRefinementAnalyst(ChatLanguageModel ollamaMistralModel) {
		return AiServices.create(QuoteRefinementAnalyst.class, ollamaMistralModel);
	}
}
