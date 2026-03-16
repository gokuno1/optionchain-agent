package com.tradebot.upstox.context;

import com.tradebot.upstox.dto.agent.DistilledFeatures;
import com.tradebot.upstox.dto.agent.OptionSignal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeatureDistillerServiceImpl implements FeatureDistillerService {

    @Override
    public DistilledFeatures distill(
            double spotPrice,
            double vix,
            String vixRegime,
            List<OptionSignal> signals
    ) {
        DistilledFeatures out = new DistilledFeatures();
        out.setSpotPrice(spotPrice);
        out.setVix(vix);
        out.setVixRegime(vixRegime != null ? vixRegime : "NORMAL");
        out.setSignals(signals != null ? new ArrayList<>(signals) : new ArrayList<>());
        return out;
    }
}
