package com.tradebot.upstox.features;

import com.tradebot.upstox.dto.agent.OptionSignal;
import com.tradebot.upstox.dto.optionchain.OptionChainData;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade over the existing {@code OptionChainRuleEngine} plus placeholders for
 * the 8 additional microstructure signals (GEX, Vanna, Charm, Entropy,
 * Wall velocity, Straddle decay, Pinning, Smart money divergence).
 *
 * This interface is intentionally minimal and additive so it does not affect
 * the existing option-chain analysis workflow.
 */
public interface OptionChainSignalService {

    /**
     * Compute all 23 microstructure signals (15 existing + 8 new).
     *
     * @param chain                filtered option chain around spot
     * @param spotPrice            current underlying spot price
     * @param vix                  current India VIX value
     * @param previousVix          previous VIX value (can be null for first run)
     * @param previousStraddleLtp  previous ATM straddle price (can be null)
     * @param expiryDate           current weekly expiry date
     * @return list of normalized option-chain signals
     */
    List<OptionSignal> computeAllSignals(
            List<OptionChainData> chain,
            double spotPrice,
            double vix,
            Double previousVix,
            Double previousStraddleLtp,
            LocalDate expiryDate
    );
}

