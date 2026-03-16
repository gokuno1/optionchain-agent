package com.tradebot.upstox.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic, LLM-friendly representation of a single option-chain signal.
 *
 * This wraps both the existing {@code OptionChainRuleSignals.*} types and
 * any new microstructure signals into a common shape that can be serialized
 * compactly into the LLM prompt.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionSignal {

    /**
     * Logical name of the signal, e.g. "OI_VELOCITY", "GEX", "PINNING_PRESSURE".
     */
    private String name;

    /**
     * Discrete state label, e.g. "SHORT_COVERING", "NEGATIVE_GEX".
     */
    private String state;

    /**
     * Normalized strength in the range [0.0, 1.0].
     */
    private double strength;

    /**
     * Additional key/value context specific to each signal
     * (support/resistance levels, corridors, zones, etc.).
     */
    private Map<String, Object> meta = new HashMap<>();
}

