package com.tradebot.upstox.context;

import com.tradebot.upstox.dto.agent.DistilledFeatures;
import com.tradebot.upstox.dto.agent.OptionSignal;
import java.util.List;

/**
 * Builds a single DistilledFeatures instance from spot, VIX, regime and signals.
 */
public interface FeatureDistillerService {

    DistilledFeatures distill(
            double spotPrice,
            double vix,
            String vixRegime,
            List<OptionSignal> signals
    );
}
