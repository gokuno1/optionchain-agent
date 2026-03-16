package com.tradebot.upstox.features;

import com.tradebot.upstox.dto.agent.OptionSignal;
import com.tradebot.upstox.dto.optionchain.OptionChainData;
import com.tradebot.upstox.dto.optionchain.OptionChainRuleSignals;
import com.tradebot.upstox.service.impl.OptionChainRuleEngine;

import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link OptionChainSignalService} that:
 * <ul>
 *     <li>Delegates to the existing {@link OptionChainRuleEngine} for the
 *         15 already-implemented signals.</li>
 *     <li>Provides stubbed placeholders for the 8 new signals
 *         (GEX, Vanna, Charm, Entropy, Wall velocity, Straddle decay,
 *         Pinning, Smart money divergence) which will be incrementally
 *         implemented.</li>
 * </ul>
 *
 * This class is additive and does not modify the existing option-chain
 * analysis workflow.
 */
@Service
public class OptionChainSignalServiceImpl implements OptionChainSignalService {

    private final OptionChainRuleEngine ruleEngine = new OptionChainRuleEngine();

    @Override
    public List<OptionSignal> computeAllSignals(
            List<OptionChainData> chain,
            double spotPrice,
            double vix,
            Double previousVix,
            Double previousStraddleLtp,
            LocalDate expiryDate
    ) {
        List<OptionSignal> signals = new ArrayList<>();

        // 1) Existing 15 signals from OptionChainRuleEngine
        OptionChainRuleSignals legacy = ruleEngine.evaluate(chain, spotPrice);
        signals.addAll(mapLegacySignals(legacy));

        // 2) 8 new signals with real calculation logic
        signals.add(computeGammaExposure(chain, spotPrice));
        signals.add(computeVannaFlow(chain, spotPrice, vix, previousVix));
        signals.add(computeCharmPressure(chain, spotPrice, expiryDate));
        signals.add(computeOiEntropy(chain));
        signals.add(computeWallVelocity(chain));
        signals.add(computeStraddleDecay(chain, spotPrice, previousStraddleLtp));
        signals.add(computePinningPressure(chain, spotPrice, expiryDate));
        signals.add(computeSmartMoneyDivergence(chain, spotPrice));

        return signals;
    }

    private List<OptionSignal> mapLegacySignals(OptionChainRuleSignals legacy) {
        List<OptionSignal> result = new ArrayList<>();
        if (legacy == null) {
            return result;
        }

        // OI Velocity
        if (legacy.getOiVelocity() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("OI_VELOCITY");
            s.setState(legacy.getOiVelocity().getState());
            s.setStrength(legacy.getOiVelocity().getStrength());
            if (legacy.getOiVelocity().getTopResistanceBand() != null) {
                s.getMeta().put("resistance_from", legacy.getOiVelocity().getTopResistanceBand().getFrom());
                s.getMeta().put("resistance_to", legacy.getOiVelocity().getTopResistanceBand().getTo());
            }
            if (legacy.getOiVelocity().getTopSupportBand() != null) {
                s.getMeta().put("support_from", legacy.getOiVelocity().getTopSupportBand().getFrom());
                s.getMeta().put("support_to", legacy.getOiVelocity().getTopSupportBand().getTo());
            }
            s.getMeta().put("call_delta_pct", legacy.getOiVelocity().getCallDeltaOiPct());
            s.getMeta().put("put_delta_pct", legacy.getOiVelocity().getPutDeltaOiPct());
            s.getMeta().put("price_move", legacy.getOiVelocity().getPriceMove());
            s.getMeta().put("atm_contraction", legacy.getOiVelocity().isAtmContractionActive());
            result.add(s);
        }

        // Opening vs Closing
        if (legacy.getOpeningVsClosing() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("OPENING_VS_CLOSING");
            s.setState(legacy.getOpeningVsClosing().getState());
            s.setStrength(legacy.getOpeningVsClosing().getStrength());
            s.getMeta().put("positional_share", legacy.getOpeningVsClosing().getPositionalVolumeShare());
            s.getMeta().put("churn_share", legacy.getOpeningVsClosing().getChurnVolumeShare());
            if (legacy.getOpeningVsClosing().getTopMagnetBand() != null) {
                s.getMeta().put("magnet_from", legacy.getOpeningVsClosing().getTopMagnetBand().getFrom());
                s.getMeta().put("magnet_to", legacy.getOpeningVsClosing().getTopMagnetBand().getTo());
            }
            result.add(s);
        }

        // Dealer Hedge Pressure
        if (legacy.getDealerHedgePressure() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("DEALER_HEDGE_PRESSURE");
            s.setState(legacy.getDealerHedgePressure().getState());
            s.setStrength(legacy.getDealerHedgePressure().getStrength());
            s.getMeta().put("net_public_call_delta", legacy.getDealerHedgePressure().getNetPublicCallDelta());
            s.getMeta().put("net_public_put_delta", legacy.getDealerHedgePressure().getNetPublicPutDelta());
            s.getMeta().put("net_dealer_delta_sensitivity", legacy.getDealerHedgePressure().getNetDealerDeltaSensitivity());
            s.getMeta().put("moneyness_center", legacy.getDealerHedgePressure().getMoneynessWeightedCenter());
            result.add(s);
        }

        // OI Center of Gravity
        if (legacy.getOiCenterOfGravity() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("OI_CENTER_OF_GRAVITY");
            s.setState(legacy.getOiCenterOfGravity().getState());
            s.setStrength(legacy.getOiCenterOfGravity().getStrength());
            s.getMeta().put("cog_strike", legacy.getOiCenterOfGravity().getCogStrike());
            s.getMeta().put("distance_from_spot", legacy.getOiCenterOfGravity().getDistanceFromSpot());
            s.getMeta().put("drift_points", legacy.getOiCenterOfGravity().getDriftPoints());
            result.add(s);
        }

        // Convexity Asymmetry
        if (legacy.getConvexityAsymmetry() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("CONVEXITY_ASYMMETRY");
            s.setState(legacy.getConvexityAsymmetry().getState());
            s.setStrength(legacy.getConvexityAsymmetry().getStrength());
            s.getMeta().put("atm_call_share", legacy.getConvexityAsymmetry().getAtmCallOiShare());
            s.getMeta().put("atm_put_share", legacy.getConvexityAsymmetry().getAtmPutOiShare());
            s.getMeta().put("otm_call_share", legacy.getConvexityAsymmetry().getOtmCallOiShare());
            s.getMeta().put("otm_put_share", legacy.getConvexityAsymmetry().getOtmPutOiShare());
            if (legacy.getConvexityAsymmetry().getDominantWingBand() != null) {
                s.getMeta().put("dominant_wing_from", legacy.getConvexityAsymmetry().getDominantWingBand().getFrom());
                s.getMeta().put("dominant_wing_to", legacy.getConvexityAsymmetry().getDominantWingBand().getTo());
            }
            result.add(s);
        }

        // Liquidity Void
        if (legacy.getLiquidityVoid() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("LIQUIDITY_VOID");
            s.setState(legacy.getLiquidityVoid().getState());
            s.setStrength(legacy.getLiquidityVoid().getStrength());
            if (legacy.getLiquidityVoid().getVoidAboveSpot() != null) {
                s.getMeta().put("void_above_from", legacy.getLiquidityVoid().getVoidAboveSpot().getFrom());
                s.getMeta().put("void_above_to", legacy.getLiquidityVoid().getVoidAboveSpot().getTo());
            }
            if (legacy.getLiquidityVoid().getVoidBelowSpot() != null) {
                s.getMeta().put("void_below_from", legacy.getLiquidityVoid().getVoidBelowSpot().getFrom());
                s.getMeta().put("void_below_to", legacy.getLiquidityVoid().getVoidBelowSpot().getTo());
            }
            if (legacy.getLiquidityVoid().getTopHighOiCluster() != null) {
                s.getMeta().put("cluster_from", legacy.getLiquidityVoid().getTopHighOiCluster().getFrom());
                s.getMeta().put("cluster_to", legacy.getLiquidityVoid().getTopHighOiCluster().getTo());
            }
            result.add(s);
        }

        // Strike Roll
        if (legacy.getStrikeRoll() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("STRIKE_ROLL");
            s.setState(legacy.getStrikeRoll().getState());
            s.setStrength(legacy.getStrikeRoll().getStrength());
            s.getMeta().put("from_strike", legacy.getStrikeRoll().getFromStrike());
            s.getMeta().put("to_strike", legacy.getStrikeRoll().getToStrike());
            s.getMeta().put("side", legacy.getStrikeRoll().getSide());
            result.add(s);
        }

        // Calendar Conflict
        if (legacy.getCalendarConflict() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("CALENDAR_CONFLICT");
            s.setState(legacy.getCalendarConflict().getState());
            s.setStrength(legacy.getCalendarConflict().getStrength());
            result.add(s);
        }

        // Skew Responsiveness
        if (legacy.getSkewResponsiveness() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("SKEW_RESPONSIVENESS");
            s.setState(legacy.getSkewResponsiveness().getState());
            s.setStrength(legacy.getSkewResponsiveness().getStrength());
            s.getMeta().put("price_move", legacy.getSkewResponsiveness().getPriceMove());
            s.getMeta().put("call_skew_slope", legacy.getSkewResponsiveness().getCallSkewSlope());
            s.getMeta().put("put_skew_slope", legacy.getSkewResponsiveness().getPutSkewSlope());
            result.add(s);
        }

        // Dealer Neutral Corridor
        if (legacy.getDealerNeutralCorridor() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("DEALER_NEUTRAL_CORRIDOR");
            s.setState(legacy.getDealerNeutralCorridor().getState());
            s.setStrength(legacy.getDealerNeutralCorridor().getStrength());
            s.getMeta().put("lower_bound", legacy.getDealerNeutralCorridor().getLowerBound());
            s.getMeta().put("upper_bound", legacy.getDealerNeutralCorridor().getUpperBound());
            result.add(s);
        }

        // Intrinsic Dominance
        if (legacy.getIntrinsicDominance() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("INTRINSIC_DOMINANCE");
            s.setState(legacy.getIntrinsicDominance().getState());
            s.setStrength(legacy.getIntrinsicDominance().getStrength());
            s.getMeta().put("intrinsic_oi_share", legacy.getIntrinsicDominance().getIntrinsicOiShare());
            result.add(s);
        }

        // Strike Defense Failure
        if (legacy.getStrikeDefenseFailure() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("STRIKE_DEFENSE_FAILURE");
            s.setState(legacy.getStrikeDefenseFailure().getState());
            s.setStrength(legacy.getStrikeDefenseFailure().getStrength());
            s.getMeta().put("failed_strike", legacy.getStrikeDefenseFailure().getFailedStrike());
            s.getMeta().put("side", legacy.getStrikeDefenseFailure().getSide());
            result.add(s);
        }

        // Synthetic Forward Distortion
        if (legacy.getSyntheticForwardDistortion() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("SYNTHETIC_FORWARD_DISTORTION");
            s.setState(legacy.getSyntheticForwardDistortion().getState());
            s.setStrength(legacy.getSyntheticForwardDistortion().getStrength());
            result.add(s);
        }

        // Participant Trap
        if (legacy.getParticipantTrap() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("PARTICIPANT_TRAP");
            s.setState(legacy.getParticipantTrap().getState());
            s.setStrength(legacy.getParticipantTrap().getStrength());
            if (legacy.getParticipantTrap().getRetailShortZone() != null) {
                s.getMeta().put("retail_short_from", legacy.getParticipantTrap().getRetailShortZone().getFrom());
                s.getMeta().put("retail_short_to", legacy.getParticipantTrap().getRetailShortZone().getTo());
            }
            if (legacy.getParticipantTrap().getInstitutionalWingZone() != null) {
                s.getMeta().put("inst_wing_from", legacy.getParticipantTrap().getInstitutionalWingZone().getFrom());
                s.getMeta().put("inst_wing_to", legacy.getParticipantTrap().getInstitutionalWingZone().getTo());
            }
            result.add(s);
        }

        // Volatility Supply Exhaustion
        if (legacy.getVolatilitySupplyExhaustion() != null) {
            OptionSignal s = new OptionSignal();
            s.setName("VOLATILITY_SUPPLY_EXHAUSTION");
            s.setState(legacy.getVolatilitySupplyExhaustion().getState());
            s.setStrength(legacy.getVolatilitySupplyExhaustion().getStrength());
            s.getMeta().put("exhaustion_score", legacy.getVolatilitySupplyExhaustion().getExhaustionScore());
            result.add(s);
        }

        return result;
    }

    // ---- Helper: ATM strike from chain --------------------------------------

    private double findClosestStrike(List<OptionChainData> chain, double spotPrice) {
        return chain.stream()
                .min(Comparator.comparingDouble(o -> Math.abs(o.getStrike_price() - spotPrice)))
                .map(OptionChainData::getStrike_price)
                .orElse(spotPrice);
    }

    @Nullable
    private OptionChainData getAtmRow(List<OptionChainData> chain, double spotPrice) {
        double atmStrike = findClosestStrike(chain, spotPrice);
        return chain.stream()
                .filter(o -> o.getStrike_price() == atmStrike)
                .findFirst()
                .orElse(null);
    }

    // ---- Signal 16: Gamma Exposure (GEX) ------------------------------------
    // GEX = Σ(callOI × callGamma × spot² × 0.01) - Σ(putOI × putGamma × spot² × 0.01)
    // normalizedGEX = GEX / spot. States: NEGATIVE_GEX, POSITIVE_GEX, NEUTRAL

    private OptionSignal computeGammaExposure(List<OptionChainData> chain, double spotPrice) {
        OptionSignal s = new OptionSignal();
        s.setName("GEX");
        if (chain == null || chain.isEmpty() || spotPrice <= 0) {
            s.setState("NEUTRAL");
            s.setStrength(0.0);
            return s;
        }
        double spotSq = spotPrice * spotPrice;
        double scale = 0.01 * spotSq;
        double callGex = 0.0;
        double putGex = 0.0;
        for (OptionChainData row : chain) {
            double callOi = row.getCall_options().getMarket_data().getOi();
            double putOi = row.getPut_options().getMarket_data().getOi();
            double callGamma = row.getCall_options().getOption_greeks().getGamma();
            double putGamma = row.getPut_options().getOption_greeks().getGamma();
            callGex += callOi * callGamma * scale;
            putGex += putOi * putGamma * scale;
        }
        double gex = callGex - putGex;
        double normalizedGex = spotPrice > 0 ? gex / spotPrice : 0.0;
        s.getMeta().put("normalized_gex", normalizedGex);
        if (normalizedGex < -1.0) {
            s.setState("NEGATIVE_GEX");
            s.setStrength(Math.min(1.0, Math.abs(normalizedGex) / 10.0));
        } else if (normalizedGex > 1.0) {
            s.setState("POSITIVE_GEX");
            s.setStrength(Math.min(1.0, normalizedGex / 10.0));
        } else {
            s.setState("NEUTRAL");
            s.setStrength(0.0);
        }
        return s;
    }

    // ---- Signal 17: Vanna Flow Pressure --------------------------------------
    // vannaFlow = Σ(OI × vega × delta / spot). VIX falling + positive flow -> BULLISH_VANNA, etc.

    private OptionSignal computeVannaFlow(List<OptionChainData> chain, double spotPrice, double vix, @Nullable Double previousVix) {
        OptionSignal s = new OptionSignal();
        s.setName("VANNA_FLOW");
        if (chain == null || chain.isEmpty() || spotPrice <= 0) {
            s.setState("NEUTRAL");
            s.setStrength(0.0);
            return s;
        }
        double vannaFlow = 0.0;
        for (OptionChainData row : chain) {
            double callOi = row.getCall_options().getMarket_data().getOi();
            double putOi = row.getPut_options().getMarket_data().getOi();
            double callVega = row.getCall_options().getOption_greeks().getVega();
            double putVega = row.getPut_options().getOption_greeks().getVega();
            double callDelta = row.getCall_options().getOption_greeks().getDelta();
            double putDelta = row.getPut_options().getOption_greeks().getDelta();
            vannaFlow += (callOi * callVega * callDelta + putOi * putVega * putDelta) / spotPrice;
        }
        s.getMeta().put("vanna_flow", vannaFlow);
        boolean vixFalling = previousVix != null && vix < previousVix;
        boolean vixRising = previousVix != null && vix > previousVix;
        if (vixFalling && vannaFlow > 0) {
            s.setState("BULLISH_VANNA");
            s.setStrength(Math.min(1.0, Math.abs(vannaFlow) / 1e6));
        } else if (vixRising && vannaFlow < 0) {
            s.setState("BEARISH_VANNA");
            s.setStrength(Math.min(1.0, Math.abs(vannaFlow) / 1e6));
        } else {
            s.setState("NEUTRAL");
            s.setStrength(0.0);
        }
        return s;
    }

    // ---- Signal 18: Charm Decay Pressure ------------------------------------
    // charmPressure = Σ(callOI × callDelta) - Σ(putOI × |putDelta|). Expiry day strength × 2.

    private OptionSignal computeCharmPressure(List<OptionChainData> chain, double spotPrice, LocalDate expiryDate) {
        OptionSignal s = new OptionSignal();
        s.setName("CHARM_PRESSURE");
        if (chain == null || chain.isEmpty()) {
            s.setState("MINIMAL");
            s.setStrength(0.0);
            return s;
        }
        double callSide = 0.0;
        double putSide = 0.0;
        for (OptionChainData row : chain) {
            double callOi = row.getCall_options().getMarket_data().getOi();
            double putOi = row.getPut_options().getMarket_data().getOi();
            double callDelta = row.getCall_options().getOption_greeks().getDelta();
            double putDelta = row.getPut_options().getOption_greeks().getDelta();
            callSide += callOi * callDelta;
            putSide += putOi * Math.abs(putDelta);
        }
        double charmPressure = callSide - putSide;
        boolean expiryDay = expiryDate != null && LocalDate.now().equals(expiryDate);
        double strength = Math.min(1.0, Math.abs(charmPressure) / 1e7);
        if (expiryDay) strength = Math.min(1.0, strength * 2.0);
        s.getMeta().put("charm_pressure", charmPressure);
        s.getMeta().put("expiry_day", expiryDay);
        if (charmPressure > 1e5) {
            s.setState("BULLISH_CHARM");
            s.setStrength(strength);
        } else if (charmPressure < -1e5) {
            s.setState("BEARISH_CHARM");
            s.setStrength(strength);
        } else {
            s.setState("MINIMAL");
            s.setStrength(0.0);
        }
        return s;
    }

    // ---- Signal 19: OI Concentration Entropy ---------------------------------
    // H = -Σ(p_i × log(p_i)), p_i = strikeOI / totalOI. Normalized H / log(N). LOW < 0.4, HIGH > 0.7

    private OptionSignal computeOiEntropy(List<OptionChainData> chain) {
        OptionSignal s = new OptionSignal();
        s.setName("OI_ENTROPY");
        if (chain == null || chain.isEmpty()) {
            s.setState("MODERATE");
            s.setStrength(0.0);
            return s;
        }
        double totalOi = 0.0;
        for (OptionChainData row : chain) {
            totalOi += row.getCall_options().getMarket_data().getOi() + row.getPut_options().getMarket_data().getOi();
        }
        if (totalOi <= 0) {
            s.setState("MODERATE");
            s.setStrength(0.0);
            return s;
        }
        int n = chain.size();
        double h = 0.0;
        for (OptionChainData row : chain) {
            double strikeOi = row.getCall_options().getMarket_data().getOi() + row.getPut_options().getMarket_data().getOi();
            double p = strikeOi / totalOi;
            if (p > 0) h -= p * Math.log(p);
        }
        double logN = Math.log(Math.max(n, 1));
        double normalizedH = logN > 0 ? h / logN : 0.0;
        s.getMeta().put("entropy", normalizedH);
        s.setStrength(Math.min(1.0, Math.max(0.0, normalizedH)));
        if (normalizedH < 0.4) {
            s.setState("LOW_ENTROPY");
        } else if (normalizedH > 0.7) {
            s.setState("HIGH_ENTROPY");
        } else {
            s.setState("MODERATE");
        }
        return s;
    }

    // ---- Signal 20: Put/Call Wall Velocity ----------------------------------
    // Top-3 CE OI strikes: sum ΔOI -> callWallVelocity. Top-3 PE: putWallVelocity. Ratio = put/call.

    private OptionSignal computeWallVelocity(List<OptionChainData> chain) {
        OptionSignal s = new OptionSignal();
        s.setName("PUT_CALL_WALL_VELOCITY");
        if (chain == null || chain.size() < 3) {
            s.setState("BALANCED");
            s.setStrength(0.0);
            return s;
        }
        List<OptionChainData> byCallOi = chain.stream()
                .sorted(Comparator.comparingDouble(o -> -o.getCall_options().getMarket_data().getOi()))
                .limit(3)
                .collect(Collectors.toList());
        List<OptionChainData> byPutOi = chain.stream()
                .sorted(Comparator.comparingDouble(o -> -o.getPut_options().getMarket_data().getOi()))
                .limit(3)
                .collect(Collectors.toList());
        double callWallVelocity = 0.0;
        for (OptionChainData row : byCallOi) {
            double oi = row.getCall_options().getMarket_data().getOi();
            double prev = row.getCall_options().getMarket_data().getPrev_oi();
            callWallVelocity += Math.max(0, oi - prev);
        }
        double putWallVelocity = 0.0;
        for (OptionChainData row : byPutOi) {
            double oi = row.getPut_options().getMarket_data().getOi();
            double prev = row.getPut_options().getMarket_data().getPrev_oi();
            putWallVelocity += Math.max(0, oi - prev);
        }
        double ratio = callWallVelocity > 0 ? putWallVelocity / callWallVelocity : (putWallVelocity > 0 ? 10.0 : 1.0);
        s.getMeta().put("put_call_ratio", ratio);
        if (ratio > 2.0) {
            s.setState("PUT_WALL_DOMINANT");
            s.setStrength(Math.min(1.0, (ratio - 2.0) / 3.0));
        } else if (ratio < 0.5) {
            s.setState("CALL_WALL_DOMINANT");
            s.setStrength(Math.min(1.0, (0.5 - ratio) / 0.5));
        } else {
            s.setState("BALANCED");
            s.setStrength(0.0);
        }
        return s;
    }

    // ---- Signal 21: Straddle Decay Rate --------------------------------------
    // Current ATM straddle = ATM CE LTP + ATM PE LTP. Compare to previous; ratio vs theta decay.

    private OptionSignal computeStraddleDecay(List<OptionChainData> chain, double spotPrice, @Nullable Double previousStraddleLtp) {
        OptionSignal s = new OptionSignal();
        s.setName("STRADDLE_DECAY");
        OptionChainData atm = getAtmRow(chain, spotPrice);
        if (atm == null) {
            s.setState("NORMAL");
            s.setStrength(0.0);
            return s;
        }
        double ceLtp = atm.getCall_options().getMarket_data().getLtp();
        double peLtp = atm.getPut_options().getMarket_data().getLtp();
        double currentStraddle = ceLtp + peLtp;
        s.getMeta().put("straddle_ltp", currentStraddle);
        if (previousStraddleLtp == null || previousStraddleLtp <= 0) {
            s.setState("NORMAL");
            s.setStrength(0.0);
            return s;
        }
        double actualDecay = previousStraddleLtp - currentStraddle;
        double atmCallTheta = atm.getCall_options().getOption_greeks().getTheta();
        double atmPutTheta = atm.getPut_options().getOption_greeks().getTheta();
        double theoreticalDecayPerCycle = Math.abs(atmCallTheta + atmPutTheta) * 15.0; // 15 min
        if (theoreticalDecayPerCycle <= 0) {
            s.setState("NORMAL");
            s.setStrength(0.0);
            return s;
        }
        double ratio = actualDecay / theoreticalDecayPerCycle;
        s.getMeta().put("decay_ratio", ratio);
        if (ratio > 1.5) {
            s.setState("FASTER_THAN_THETA");
            s.setStrength(Math.min(1.0, (ratio - 1.5) / 1.5));
        } else if (ratio < 0.5 && actualDecay > 0) {
            s.setState("SLOWER_THAN_THETA");
            s.setStrength(Math.min(1.0, (0.5 - ratio) / 0.5));
        } else {
            s.setState("NORMAL");
            s.setStrength(0.0);
        }
        return s;
    }

    // ---- Signal 22: Pinning Pressure -----------------------------------------
    // Max pain = strike that maximizes option buyer loss. pinScore = clamp(1 - |spot - maxPain|/(0.01*spot), 0, 1).

    private OptionSignal computePinningPressure(List<OptionChainData> chain, double spotPrice, LocalDate expiryDate) {
        OptionSignal s = new OptionSignal();
        s.setName("PINNING_PRESSURE");
        if (chain == null || chain.isEmpty() || spotPrice <= 0) {
            s.setState("LOW_PINNING");
            s.setStrength(0.0);
            return s;
        }
        List<Double> strikes = chain.stream().map(OptionChainData::getStrike_price).distinct().sorted().collect(Collectors.toList());
        double maxPain = spotPrice;
        double maxPainValue = Double.NEGATIVE_INFINITY;
        for (Double S : strikes) {
            double total = 0.0;
            for (OptionChainData row : chain) {
                double K = row.getStrike_price();
                double callOi = row.getCall_options().getMarket_data().getOi();
                double putOi = row.getPut_options().getMarket_data().getOi();
                total += callOi * Math.max(0, S - K) + putOi * Math.max(0, K - S);
            }
            if (total > maxPainValue) {
                maxPainValue = total;
                maxPain = S;
            }
        }
        double pinScore = 1.0 - Math.min(1.0, Math.abs(spotPrice - maxPain) / (0.01 * spotPrice));
        pinScore = Math.max(0.0, Math.min(1.0, pinScore));
        boolean expiryDay = expiryDate != null && LocalDate.now().equals(expiryDate);
        if (expiryDay) pinScore = Math.min(1.0, pinScore * 1.5);
        s.getMeta().put("max_pain", maxPain);
        s.getMeta().put("pin_score", pinScore);
        s.setStrength(pinScore);
        if (pinScore > 0.7) {
            s.setState("HIGH_PINNING");
        } else if (pinScore >= 0.3) {
            s.setState("MODERATE_PINNING");
        } else {
            s.setState("LOW_PINNING");
        }
        return s;
    }

    // ---- Signal 23: Smart Money Divergence ----------------------------------
    // Top-5 volume strikes: volume-weighted call vs put bias. Compare to spot direction.

    private OptionSignal computeSmartMoneyDivergence(List<OptionChainData> chain, double spotPrice) {
        OptionSignal s = new OptionSignal();
        s.setName("SMART_MONEY_DIVERGENCE");
        if (chain == null || chain.isEmpty()) {
            s.setState("ALIGNED");
            s.setStrength(0.0);
            return s;
        }
        List<OptionChainData> byVolume = chain.stream()
                .sorted(Comparator.comparingDouble(o ->
                        -(o.getCall_options().getMarket_data().getVolume() + o.getPut_options().getMarket_data().getVolume())))
                .limit(5)
                .collect(Collectors.toList());
        double callBias = 0.0;
        double putBias = 0.0;
        for (OptionChainData row : byVolume) {
            double callVol = row.getCall_options().getMarket_data().getVolume();
            double putVol = row.getPut_options().getMarket_data().getVolume();
            double callDeltaOi = row.getCall_options().getMarket_data().getOi() - row.getCall_options().getMarket_data().getPrev_oi();
            double putDeltaOi = row.getPut_options().getMarket_data().getOi() - row.getPut_options().getMarket_data().getPrev_oi();
            if (callDeltaOi > 0) callBias += callVol;
            if (putDeltaOi > 0) putBias += putVol;
        }
        double spotRef = chain.get(0).getCall_options().getMarket_data().getClose_price();
        boolean spotUp = spotPrice > spotRef;
        boolean spotDown = spotPrice < spotRef;
        s.getMeta().put("call_bias", callBias);
        s.getMeta().put("put_bias", putBias);
        boolean callHeavy = callBias > putBias;
        boolean putHeavy = putBias > callBias;
        if (spotDown && callHeavy) {
            s.setState("SMART_BULLISH_DIVERGE");
            s.setStrength(Math.min(1.0, Math.abs(callBias - putBias) / Math.max(callBias + putBias, 1)));
        } else if (spotUp && putHeavy) {
            s.setState("SMART_BEARISH_DIVERGE");
            s.setStrength(Math.min(1.0, Math.abs(putBias - callBias) / Math.max(callBias + putBias, 1)));
        } else {
            s.setState("ALIGNED");
            s.setStrength(0.0);
        }
        return s;
    }
}

