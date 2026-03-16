package com.tradebot.upstox.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebot.upstox.context.FeatureDistillerService;
import com.tradebot.upstox.context.VixRegimeService;
import com.tradebot.upstox.dto.agent.AgentDecision;
import com.tradebot.upstox.dto.agent.AgentOptionChainInput;
import com.tradebot.upstox.dto.agent.DistilledFeatures;
import com.tradebot.upstox.dto.agent.OptionSignal;
import com.tradebot.upstox.features.OptionChainSignalService;
import com.tradebot.upstox.llm.AgentDecisionAnalyst;
import com.tradebot.upstox.service.OptionChainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Builds distilled features for the intraday agent by combining option chain data,
 * 23 microstructure signals and VIX regime.
 */
@Service
@Slf4j
public class AgentAnalysisService {

    @Autowired
    private OptionChainService optionChainService;
    @Autowired
    private OptionChainSignalService optionChainSignalService;
    @Autowired
    private VixRegimeService vixRegimeService;
    @Autowired
    private FeatureDistillerService featureDistillerService;
    @Autowired
    private AgentDecisionAnalyst agentDecisionAnalyst;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Fetches option chain data, computes all signals and regime, and returns
     * distilled features. Previous snapshot is optional (e.g. from cache) for
     * change detection.
     *
     * @param index       e.g. "Nifty 50"
     * @param expiryDate   YYYY-MM-DD
     * @param userId       Upstox user id
     * @param previous     previous DistilledFeatures for delta score (null = first run)
     * @param positionState optional open position state
     * @return empty if chain fetch fails, else distilled features
     */
    public Optional<DistilledFeatures> buildDistilledFeatures(
            String index,
            String expiryDate,
            String userId
    ) {
        Optional<AgentOptionChainInput> inputOpt =
                optionChainService.getOptionChainDataForAgent(index, expiryDate, userId);
        if (inputOpt.isEmpty()) {
            log.warn("No option chain data for agent: index={}, expiry={}", index, expiryDate);
            return Optional.empty();
        }
        AgentOptionChainInput input = inputOpt.get();
        if (input.getFilteredChain() == null || input.getFilteredChain().isEmpty()) {
            log.info("no response from getOptionChainDataForAgent");
            return Optional.empty();
        }

        LocalDate expiry = LocalDate.parse(input.getExpiryDate());
        List<OptionSignal> signals = optionChainSignalService.computeAllSignals(
                input.getFilteredChain(),
                input.getSpotPrice(),
                input.getVix(),
                null,
                null,
                expiry
        );

        String vixRegime = vixRegimeService.determineRegime(input.getVix(), null);

        DistilledFeatures current = featureDistillerService.distill(
                input.getSpotPrice(),
                input.getVix(),
                vixRegime,
                signals
        );
        return Optional.of(current);
    }

    /**
     * Build distilled features and, if deltaScore exceeds the threshold, call the LLM
     * to obtain a structured BUY_CE / BUY_PE / HOLD decision.
     */
    public Optional<AgentDecision> inferDecision(
            String index,
            String expiryDate,
            String userId
    ) {
        log.info("started inferDecision process");
        Optional<DistilledFeatures> featuresOpt =
                buildDistilledFeatures(index, expiryDate, userId);
        if (featuresOpt.isEmpty()) {
            log.info("empty distilled features");
            return Optional.empty();
        }
        DistilledFeatures features = featuresOpt.get();
//        if (!shouldCallLlm(features)) {
//            log.info("shouldCallLlm is false");
//            return Optional.empty();
//        }
        try {
            String featuresJson = objectMapper.writeValueAsString(features);
            log.info("making agent call with features {}", featuresJson);
            AgentDecision decision = agentDecisionAnalyst.decide(index, expiryDate, featuresJson);
            log.info("agent decision received. {}", decision);
            return Optional.ofNullable(decision);
        } catch (Exception e) {
            log.error("Failed to invoke agent LLM decision: {}", e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
