package com.tradebot.upstox.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured LLM decision for the intraday agent, derived from DistilledFeatures.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentDecision {

    /**
     * BUY_CE, BUY_PE, or HOLD.
     */
    private String action;

    /**
     * Confidence band, e.g. "low", "medium", "high" or "neutral".
     */
    private String confidence;

    /**
     * Selected option strike (multiple of 50), or 0 for HOLD.
     */
    private double strike;

    /**
     * Suggested entry price band [low, high].
     */
    private List<Double> entryPriceRange;

    private double stopLoss;
    private double target;

    /**
     * Short natural-language explanation (2–3 sentences).
     */
    private String reasoning;

    /**
     * Key factors that drove the decision (e.g. NEGATIVE_GEX, SHORT_COVERING).
     */
    private List<String> keyFactors;
}

