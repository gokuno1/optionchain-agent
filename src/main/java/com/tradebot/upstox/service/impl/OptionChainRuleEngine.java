package com.tradebot.upstox.service.impl;

import com.tradebot.upstox.dto.optionchain.MarketData;
import com.tradebot.upstox.dto.optionchain.OptionChainData;
import com.tradebot.upstox.dto.optionchain.OptionChainRuleSignals;
import com.tradebot.upstox.dto.optionchain.OptionGreeks;

import java.util.Comparator;
import java.util.List;

public class OptionChainRuleEngine {

	public OptionChainRuleSignals evaluate(List<OptionChainData> chain, double spotPrice) {
		OptionChainRuleSignals signals = new OptionChainRuleSignals();
		if (chain == null || chain.isEmpty()) {
			return signals;
		}
		signals.setOiVelocity(computeOiVelocity(chain, spotPrice));
		signals.setOpeningVsClosing(computeOpeningVsClosing(chain, spotPrice));
		signals.setDealerHedgePressure(computeDealerHedgePressure(chain, spotPrice));
		signals.setOiCenterOfGravity(computeOiCenterOfGravity(chain, spotPrice));
		signals.setConvexityAsymmetry(computeConvexityAsymmetry(chain, spotPrice));
		signals.setLiquidityVoid(computeLiquidityVoid(chain, spotPrice));
		signals.setStrikeRoll(computeStrikeRoll(chain));
		signals.setCalendarConflict(neutralCalendarConflict());
		signals.setSkewResponsiveness(computeSkewResponsiveness(chain, spotPrice));
		signals.setDealerNeutralCorridor(computeDealerNeutralCorridor(chain, spotPrice));
		signals.setIntrinsicDominance(computeIntrinsicDominance(chain, spotPrice));
		signals.setStrikeDefenseFailure(computeStrikeDefenseFailure(chain, spotPrice));
		signals.setSyntheticForwardDistortion(neutralSyntheticForwardDistortion());
		signals.setParticipantTrap(computeParticipantTrap(chain, spotPrice));
		signals.setVolatilitySupplyExhaustion(computeVolatilitySupplyExhaustion(chain, spotPrice));
		return signals;
	}

	private OptionChainRuleSignals.OiVelocitySignal computeOiVelocity(List<OptionChainData> chain, double spotPrice) {
		double callDeltaOiSum = 0.0;
		double putDeltaOiSum = 0.0;
		double callOiSum = 0.0;
		double putOiSum = 0.0;
		for (OptionChainData data : chain) {
			MarketData callMd = data.getCall_options().getMarket_data();
			MarketData putMd = data.getPut_options().getMarket_data();
			callDeltaOiSum += callMd.getOi() - callMd.getPrev_oi();
			putDeltaOiSum += putMd.getOi() - putMd.getPrev_oi();
			callOiSum += callMd.getOi();
			putOiSum += putMd.getOi();
		}
		OptionChainRuleSignals.OiVelocitySignal signal = new OptionChainRuleSignals.OiVelocitySignal();
		double callDeltaPct = (callOiSum != 0.0) ? callDeltaOiSum / callOiSum : 0.0;
		double putDeltaPct = (putOiSum != 0.0) ? putDeltaOiSum / putOiSum : 0.0;
		signal.setCallDeltaOiPct(callDeltaPct);
		signal.setPutDeltaOiPct(putDeltaPct);
		double firstClose = chain.get(0).getCall_options().getMarket_data().getClose_price();
		String priceMove = chain.get(0).getUnderlying_spot_price() - firstClose > 0 ? "UP" : chain.get(0).getUnderlying_spot_price() - firstClose < 0 ? "DOWN" : "FLAT";
		signal.setPriceMove(priceMove);
		String state = "NEUTRAL";
		if ("UP".equals(priceMove)) {
			if (callDeltaPct > 0.05) state = "RESISTANCE_FORMATION";
			else if (callDeltaPct < -0.05) state = "SHORT_COVERING";
		} else if ("DOWN".equals(priceMove)) {
			if (putDeltaPct > 0.05) state = "BEARISH_POSITIONING";
			else if (putDeltaPct < -0.05) state = "FLOOR_REMOVAL";
		}
		signal.setState(state);
		signal.setStrength(Math.min(1.0, Math.abs(callDeltaPct) + Math.abs(putDeltaPct)));
		double atmStrike = findClosestStrike(chain, spotPrice);
		double atmCallDeltaOi = 0.0;
		double atmPutDeltaOi = 0.0;
		for (OptionChainData data : chain) {
			if (data.getStrike_price() == atmStrike) {
				atmCallDeltaOi = data.getCall_options().getMarket_data().getOi() - data.getCall_options().getMarket_data().getPrev_oi();
				atmPutDeltaOi = data.getPut_options().getMarket_data().getOi() - data.getPut_options().getMarket_data().getPrev_oi();
				break;
			}
		}
		signal.setAtmContractionActive(atmCallDeltaOi < 0 && atmPutDeltaOi < 0);
		double maxCallDeltaOi = Double.NEGATIVE_INFINITY;
		double maxPutDeltaOi = Double.NEGATIVE_INFINITY;
		double resStrike = atmStrike;
		double supStrike = atmStrike;
		for (OptionChainData data : chain) {
			double callDeltaOi = data.getCall_options().getMarket_data().getOi() - data.getCall_options().getMarket_data().getPrev_oi();
			double putDeltaOi = data.getPut_options().getMarket_data().getOi() - data.getPut_options().getMarket_data().getPrev_oi();
			if (callDeltaOi > maxCallDeltaOi) {
				maxCallDeltaOi = callDeltaOi;
				resStrike = data.getStrike_price();
			}
			if (putDeltaOi > maxPutDeltaOi) {
				maxPutDeltaOi = putDeltaOi;
				supStrike = data.getStrike_price();
			}
		}
		signal.setTopResistanceBand(new OptionChainRuleSignals.StrikeBand(resStrike, resStrike));
		signal.setTopSupportBand(new OptionChainRuleSignals.StrikeBand(supStrike, supStrike));
		return signal;
	}

	private OptionChainRuleSignals.OpeningVsClosingSignal computeOpeningVsClosing(List<OptionChainData> chain, double spotPrice) {
		double positionalVolume = 0.0;
		double churnVolume = 0.0;
		for (OptionChainData data : chain) {
			MarketData callMd = data.getCall_options().getMarket_data();
			MarketData putMd = data.getPut_options().getMarket_data();
			int callVol = callMd.getVolume();
			int putVol = putMd.getVolume();
			double callDeltaOi = callMd.getOi() - callMd.getPrev_oi();
			double putDeltaOi = putMd.getOi() - putMd.getPrev_oi();
			double effCall = (callVol > 0) ? Math.abs(callDeltaOi) / callVol : 0.0;
			double effPut = (putVol > 0) ? Math.abs(putDeltaOi) / putVol : 0.0;
			if (effCall > 0.2) positionalVolume += callVol; else churnVolume += callVol;
			if (effPut > 0.2) positionalVolume += putVol; else churnVolume += putVol;
		}
		OptionChainRuleSignals.OpeningVsClosingSignal signal = new OptionChainRuleSignals.OpeningVsClosingSignal();
		double total = positionalVolume + churnVolume;
		signal.setPositionalVolumeShare(total > 0 ? positionalVolume / total : 0.0);
		signal.setChurnVolumeShare(total > 0 ? churnVolume / total : 0.0);
		signal.setStrength(Math.min(1.0, Math.abs(signal.getPositionalVolumeShare() - signal.getChurnVolumeShare())));
		String state = "MIXED";
		if (signal.getPositionalVolumeShare() > 0.6) state = "POSITIONAL_DOMINANT";
		else if (signal.getChurnVolumeShare() > 0.6) state = "CHURN_DOMINANT";
		signal.setState(state);
		double bestScore = Double.NEGATIVE_INFINITY;
		double bestStrike = findClosestStrike(chain, spotPrice);
		for (OptionChainData data : chain) {
			double callDeltaOi = Math.max(0.0, data.getCall_options().getMarket_data().getOi() - data.getCall_options().getMarket_data().getPrev_oi());
			double putDeltaOi = Math.max(0.0, data.getPut_options().getMarket_data().getOi() - data.getPut_options().getMarket_data().getPrev_oi());
			double score = callDeltaOi + putDeltaOi;
			if (score > bestScore) {
				bestScore = score;
				bestStrike = data.getStrike_price();
			}
		}
		signal.setTopMagnetBand(new OptionChainRuleSignals.StrikeBand(bestStrike, bestStrike));
		return signal;
	}

	private OptionChainRuleSignals.DealerHedgePressureSignal computeDealerHedgePressure(List<OptionChainData> chain, double spotPrice) {
		double netCallDelta = 0.0;
		double netPutDelta = 0.0;
		double weightedStrikeSum = 0.0;
		double weightSum = 0.0;
		for (OptionChainData data : chain) {
			double strike = data.getStrike_price();
			OptionGreeks callGreeks = data.getCall_options().getOption_greeks();
			OptionGreeks putGreeks = data.getPut_options().getOption_greeks();
			MarketData callMd = data.getCall_options().getMarket_data();
			MarketData putMd = data.getPut_options().getMarket_data();
			double moneynessWeight = Math.exp(-Math.abs(strike - spotPrice) / Math.max(spotPrice, 1.0));
			netCallDelta += callMd.getOi() * callGreeks.getDelta() * moneynessWeight;
			netPutDelta += putMd.getOi() * putGreeks.getDelta() * moneynessWeight;
			weightedStrikeSum += strike * moneynessWeight;
			weightSum += moneynessWeight;
		}
		OptionChainRuleSignals.DealerHedgePressureSignal signal = new OptionChainRuleSignals.DealerHedgePressureSignal();
		signal.setNetPublicCallDelta(netCallDelta);
		signal.setNetPublicPutDelta(netPutDelta);
		double netExposure = netCallDelta + netPutDelta;
		signal.setNetDealerDeltaSensitivity(Math.abs(netExposure));
		signal.setMoneynessWeightedCenter(weightSum > 0.0 ? weightedStrikeSum / weightSum : spotPrice);
		signal.setState(netExposure > 0 ? "MOMENTUM_REINFORCEMENT_UP" : netExposure < 0 ? "MOMENTUM_REINFORCEMENT_DOWN" : "LOW_HEDGE_PRESSURE");
		signal.setStrength(Math.min(1.0, Math.abs(netExposure) / Math.max(spotPrice, 1.0)));
		return signal;
	}

	private OptionChainRuleSignals.OiCenterOfGravitySignal computeOiCenterOfGravity(List<OptionChainData> chain, double spotPrice) {
		double totalOi = 0.0;
		double weightedStrike = 0.0;
		for (OptionChainData data : chain) {
			double total = data.getCall_options().getMarket_data().getOi() + data.getPut_options().getMarket_data().getOi();
			totalOi += total;
			weightedStrike += total * data.getStrike_price();
		}
		double cog = totalOi > 0.0 ? weightedStrike / totalOi : spotPrice;
		OptionChainRuleSignals.OiCenterOfGravitySignal signal = new OptionChainRuleSignals.OiCenterOfGravitySignal();
		signal.setCogStrike(cog);
		signal.setPrevCogStrike(null);
		double distance = cog - spotPrice;
		signal.setDistanceFromSpot(distance);
		signal.setState(distance > 0 ? "COG_ABOVE_SPOT" : distance < 0 ? "COG_BELOW_SPOT" : "STATIC");
		signal.setDriftPoints(Math.abs(distance));
		signal.setStrength(Math.min(1.0, Math.abs(distance) / Math.max(spotPrice, 1.0)));
		return signal;
	}

	private OptionChainRuleSignals.ConvexityAsymmetrySignal computeConvexityAsymmetry(List<OptionChainData> chain, double spotPrice) {
		double atmCallOi = 0.0, atmPutOi = 0.0, otmCallOi = 0.0, otmPutOi = 0.0, totalCallOi = 0.0, totalPutOi = 0.0;
		double atmBand = spotPrice * 0.01;
		for (OptionChainData data : chain) {
			double strike = data.getStrike_price();
			double callOi = data.getCall_options().getMarket_data().getOi();
			double putOi = data.getPut_options().getMarket_data().getOi();
			totalCallOi += callOi;
			totalPutOi += putOi;
			if (Math.abs(strike - spotPrice) <= atmBand) {
				atmCallOi += callOi;
				atmPutOi += putOi;
			} else if (strike < spotPrice) {
				otmPutOi += putOi;
			} else {
				otmCallOi += callOi;
			}
		}
		OptionChainRuleSignals.ConvexityAsymmetrySignal signal = new OptionChainRuleSignals.ConvexityAsymmetrySignal();
		signal.setAtmCallOiShare(totalCallOi > 0.0 ? atmCallOi / totalCallOi : 0.0);
		signal.setAtmPutOiShare(totalPutOi > 0.0 ? atmPutOi / totalPutOi : 0.0);
		signal.setOtmCallOiShare(totalCallOi > 0.0 ? otmCallOi / totalCallOi : 0.0);
		signal.setOtmPutOiShare(totalPutOi > 0.0 ? otmPutOi / totalPutOi : 0.0);
		String state = "BALANCED";
		double strength = 0.0;
		if (signal.getOtmPutOiShare() > 0.25) {
			state = "TAIL_RISK_PUT_DEMAND";
			strength = signal.getOtmPutOiShare();
		} else if (signal.getAtmCallOiShare() > 0.4 && signal.getOtmCallOiShare() < 0.2) {
			state = "ATM_CALL_DOMINANCE";
			strength = signal.getAtmCallOiShare();
		}
		signal.setState(state);
		signal.setStrength(Math.min(1.0, strength));
		OptionChainData farthest = chain.stream().max(Comparator.comparingDouble(o -> Math.abs(o.getStrike_price() - spotPrice))).orElse(chain.get(0));
		signal.setDominantWingBand(new OptionChainRuleSignals.StrikeBand(farthest.getStrike_price(), farthest.getStrike_price()));
		return signal;
	}

	private OptionChainRuleSignals.LiquidityVoidSignal computeLiquidityVoid(List<OptionChainData> chain, double spotPrice) {
		double maxOi = 0.0;
		for (OptionChainData data : chain) {
			double totalOi = data.getCall_options().getMarket_data().getOi() + data.getPut_options().getMarket_data().getOi();
			if (totalOi > maxOi) maxOi = totalOi;
		}
		double voidAboveFrom = Double.NaN, voidAboveTo = Double.NaN, voidBelowFrom = Double.NaN, voidBelowTo = Double.NaN;
		double clusterFrom = Double.NaN, clusterTo = Double.NaN;
		for (OptionChainData data : chain) {
			double strike = data.getStrike_price();
			double totalOi = data.getCall_options().getMarket_data().getOi() + data.getPut_options().getMarket_data().getOi();
			double pct = maxOi > 0.0 ? totalOi / maxOi : 0.0;
			if (pct < 0.2) {
				if (strike > spotPrice) {
					if (Double.isNaN(voidAboveFrom)) voidAboveFrom = strike;
					voidAboveTo = strike;
				} else {
					if (Double.isNaN(voidBelowFrom)) voidBelowFrom = strike;
					voidBelowTo = strike;
				}
			} else if (pct > 0.7) {
				if (Double.isNaN(clusterFrom)) clusterFrom = strike;
				clusterTo = strike;
			}
		}
		OptionChainRuleSignals.LiquidityVoidSignal signal = new OptionChainRuleSignals.LiquidityVoidSignal();
		signal.setState(!Double.isNaN(voidAboveFrom) ? "VOID_ABOVE_SPOT" : !Double.isNaN(voidBelowFrom) ? "VOID_BELOW_SPOT" : "NO_SIGNIFICANT_VOID");
		signal.setStrength(0.5);
		signal.setVoidAboveSpot(!Double.isNaN(voidAboveFrom) ? new OptionChainRuleSignals.StrikeBand(voidAboveFrom, voidAboveTo) : null);
		signal.setVoidBelowSpot(!Double.isNaN(voidBelowFrom) ? new OptionChainRuleSignals.StrikeBand(voidBelowFrom, voidBelowTo) : null);
		signal.setTopHighOiCluster(!Double.isNaN(clusterFrom) ? new OptionChainRuleSignals.StrikeBand(clusterFrom, clusterTo) : null);
		return signal;
	}

	private OptionChainRuleSignals.StrikeRollSignal computeStrikeRoll(List<OptionChainData> chain) {
		double bestFromStrike = Double.NaN, bestToStrike = Double.NaN, bestMagnitude = 0.0;
		for (int i = 0; i < chain.size(); i++) {
			OptionChainData from = chain.get(i);
			double fromDeltaOi = from.getCall_options().getMarket_data().getOi() - from.getCall_options().getMarket_data().getPrev_oi();
			if (fromDeltaOi < -0.0) {
				for (int j = 0; j < chain.size(); j++) {
					if (i == j) continue;
					OptionChainData to = chain.get(j);
					double toDeltaOi = to.getCall_options().getMarket_data().getOi() - to.getCall_options().getMarket_data().getPrev_oi();
					if (toDeltaOi > 0.0) {
						double mag = Math.min(Math.abs(fromDeltaOi), toDeltaOi);
						if (mag > bestMagnitude) {
							bestMagnitude = mag;
							bestFromStrike = from.getStrike_price();
							bestToStrike = to.getStrike_price();
						}
					}
				}
			}
		}
		OptionChainRuleSignals.StrikeRollSignal signal = new OptionChainRuleSignals.StrikeRollSignal();
		if (bestMagnitude == 0.0) {
			signal.setState("NONE");
			signal.setStrength(0.0);
			return signal;
		}
		signal.setFromStrike(bestFromStrike);
		signal.setToStrike(bestToStrike);
		signal.setState(bestToStrike > bestFromStrike ? "UPWARD_CALL_ROLL" : "DOWNWARD_CALL_ROLL");
		signal.setSide("CALL");
		signal.setStrength(Math.min(1.0, bestMagnitude / 100000.0));
		return signal;
	}

	private OptionChainRuleSignals.CalendarConflictSignal neutralCalendarConflict() {
		OptionChainRuleSignals.CalendarConflictSignal signal = new OptionChainRuleSignals.CalendarConflictSignal();
		signal.setState("INCONCLUSIVE");
		signal.setStrength(0.0);
		return signal;
	}

	private OptionChainRuleSignals.SkewResponsivenessSignal computeSkewResponsiveness(List<OptionChainData> chain, double spotPrice) {
		double sumCallNum = 0.0, sumCallDen = 0.0, sumPutNum = 0.0, sumPutDen = 0.0;
		for (OptionChainData data : chain) {
			double m = (data.getStrike_price() - spotPrice) / Math.max(spotPrice, 1.0);
			sumCallNum += m * data.getCall_options().getOption_greeks().getIv();
			sumCallDen += m * m;
			sumPutNum += m * data.getPut_options().getOption_greeks().getIv();
			sumPutDen += m * m;
		}
		double callSlope = (sumCallDen != 0.0) ? sumCallNum / sumCallDen : 0.0;
		double putSlope = (sumPutDen != 0.0) ? sumPutNum / sumPutDen : 0.0;
		OptionChainRuleSignals.SkewResponsivenessSignal signal = new OptionChainRuleSignals.SkewResponsivenessSignal();
		signal.setCallSkewSlope(callSlope);
		signal.setPutSkewSlope(putSlope);
		double priceChange = chain.get(0).getUnderlying_spot_price() - chain.get(0).getCall_options().getMarket_data().getClose_price();
		signal.setPriceMove(priceChange > 0 ? "UP" : priceChange < 0 ? "DOWN" : "FLAT");
		String state = "NEUTRAL";
		if ("DOWN".equals(signal.getPriceMove()) && putSlope > 0.05) state = "CONFIRMING";
		else if ("DOWN".equals(signal.getPriceMove()) && putSlope <= 0.0) state = "DIVERGING";
		else if ("UP".equals(signal.getPriceMove()) && callSlope > 0.05) state = "CHASE_RISK";
		signal.setState(state);
		signal.setStrength(Math.min(1.0, Math.abs(callSlope) + Math.abs(putSlope)));
		return signal;
	}

	private OptionChainRuleSignals.DealerNeutralCorridorSignal computeDealerNeutralCorridor(List<OptionChainData> chain, double spotPrice) {
		double lower = spotPrice, upper = spotPrice;
		for (OptionChainData data : chain) {
			double net = data.getCall_options().getOption_greeks().getDelta() + data.getPut_options().getOption_greeks().getDelta();
			if (Math.abs(net) < 0.05) {
				if (data.getStrike_price() < lower) lower = data.getStrike_price();
				if (data.getStrike_price() > upper) upper = data.getStrike_price();
			}
		}
		OptionChainRuleSignals.DealerNeutralCorridorSignal signal = new OptionChainRuleSignals.DealerNeutralCorridorSignal();
		signal.setLowerBound(lower);
		signal.setUpperBound(upper);
		signal.setState((upper - lower) <= (spotPrice * 0.02) ? "NARROW" : "WIDE");
		signal.setStrength(Math.min(1.0, spotPrice > 0 ? (upper - lower) / spotPrice : 0.0));
		return signal;
	}

	private OptionChainRuleSignals.IntrinsicDominanceSignal computeIntrinsicDominance(List<OptionChainData> chain, double spotPrice) {
		double intrinsicOi = 0.0, totalOi = 0.0;
		for (OptionChainData data : chain) {
			double strike = data.getStrike_price();
			double callIntrinsic = Math.max(0.0, spotPrice - strike);
			double putIntrinsic = Math.max(0.0, strike - spotPrice);
			if (callIntrinsic > 0.15 * strike) intrinsicOi += data.getCall_options().getMarket_data().getOi();
			if (putIntrinsic > 0.15 * strike) intrinsicOi += data.getPut_options().getMarket_data().getOi();
			totalOi += data.getCall_options().getMarket_data().getOi() + data.getPut_options().getMarket_data().getOi();
		}
		double share = totalOi > 0.0 ? intrinsicOi / totalOi : 0.0;
		OptionChainRuleSignals.IntrinsicDominanceSignal signal = new OptionChainRuleSignals.IntrinsicDominanceSignal();
		signal.setIntrinsicOiShare(share);
		signal.setState(share > 0.4 ? "INTRINSIC_DOMINANT" : "PREMIUM_DOMINANT");
		signal.setStrength(Math.min(1.0, share));
		return signal;
	}

	private OptionChainRuleSignals.StrikeDefenseFailureSignal computeStrikeDefenseFailure(List<OptionChainData> chain, double spotPrice) {
		double maxCallOi = 0.0;
		double levelStrike = spotPrice;
		MarketData levelMd = null;
		for (OptionChainData data : chain) {
			MarketData callMd = data.getCall_options().getMarket_data();
			if (callMd.getOi() > maxCallOi) {
				maxCallOi = callMd.getOi();
				levelStrike = data.getStrike_price();
				levelMd = callMd;
			}
		}
		OptionChainRuleSignals.StrikeDefenseFailureSignal signal = new OptionChainRuleSignals.StrikeDefenseFailureSignal();
		if (levelMd == null) {
			signal.setState("NONE");
			signal.setStrength(0.0);
			return signal;
		}
		double deltaOi = levelMd.getOi() - levelMd.getPrev_oi();
		if (spotPrice > levelStrike && deltaOi < 0) {
			signal.setState("RESISTANCE_FAILURE");
			signal.setFailedStrike(levelStrike);
			signal.setSide("CALL");
			signal.setStrength(Math.min(1.0, Math.abs(deltaOi) / Math.max(maxCallOi, 1.0)));
		} else if (spotPrice < levelStrike && deltaOi < 0) {
			signal.setState("SUPPORT_FAILURE");
			signal.setFailedStrike(levelStrike);
			signal.setSide("PUT");
			signal.setStrength(Math.min(1.0, Math.abs(deltaOi) / Math.max(maxCallOi, 1.0)));
		} else {
			signal.setState("NONE");
			signal.setStrength(0.0);
		}
		return signal;
	}

	private OptionChainRuleSignals.SyntheticForwardDistortionSignal neutralSyntheticForwardDistortion() {
		OptionChainRuleSignals.SyntheticForwardDistortionSignal signal = new OptionChainRuleSignals.SyntheticForwardDistortionSignal();
		signal.setState("NEAR_PARITY");
		signal.setStrength(0.0);
		return signal;
	}

	private OptionChainRuleSignals.ParticipantTrapSignal computeParticipantTrap(List<OptionChainData> chain, double spotPrice) {
		double upperTrapFrom = Double.NaN, upperTrapTo = Double.NaN, lowerTrapFrom = Double.NaN, lowerTrapTo = Double.NaN;
		for (OptionChainData data : chain) {
			double strike = data.getStrike_price();
			double callDeltaOi = data.getCall_options().getMarket_data().getOi() - data.getCall_options().getMarket_data().getPrev_oi();
			double putDeltaOi = data.getPut_options().getMarket_data().getOi() - data.getPut_options().getMarket_data().getPrev_oi();
			if (strike > spotPrice * 1.03 && callDeltaOi > 0) {
				if (Double.isNaN(upperTrapFrom)) upperTrapFrom = strike;
				upperTrapTo = strike;
			}
			if (strike < spotPrice * 0.97 && putDeltaOi > 0) {
				if (Double.isNaN(lowerTrapFrom)) lowerTrapFrom = strike;
				lowerTrapTo = strike;
			}
		}
		OptionChainRuleSignals.ParticipantTrapSignal signal = new OptionChainRuleSignals.ParticipantTrapSignal();
		signal.setState(!Double.isNaN(upperTrapFrom) ? "RETAIL_SHORT_OTM_TRAP_UP" : !Double.isNaN(lowerTrapFrom) ? "RETAIL_SHORT_OTM_TRAP_DOWN" : "NONE");
		signal.setStrength(0.5);
		signal.setRetailShortZone(!Double.isNaN(upperTrapFrom) ? new OptionChainRuleSignals.StrikeBand(upperTrapFrom, upperTrapTo) : null);
		signal.setInstitutionalWingZone(!Double.isNaN(lowerTrapFrom) ? new OptionChainRuleSignals.StrikeBand(lowerTrapFrom, lowerTrapTo) : null);
		return signal;
	}

	private OptionChainRuleSignals.VolatilitySupplyExhaustionSignal computeVolatilitySupplyExhaustion(List<OptionChainData> chain, double spotPrice) {
		double atmStrike = findClosestStrike(chain, spotPrice);
		double atmCallDeltaOi = 0.0, atmPutDeltaOi = 0.0;
		for (OptionChainData data : chain) {
			if (data.getStrike_price() == atmStrike) {
				atmCallDeltaOi = data.getCall_options().getMarket_data().getOi() - data.getCall_options().getMarket_data().getPrev_oi();
				atmPutDeltaOi = data.getPut_options().getMarket_data().getOi() - data.getPut_options().getMarket_data().getPrev_oi();
				break;
			}
		}
		double exhaustionScore = (Math.abs(atmCallDeltaOi) < 0.05 * Math.max(1.0, atmCallDeltaOi) && Math.abs(atmPutDeltaOi) < 0.05 * Math.max(1.0, atmPutDeltaOi)) ? 0.7 : 0.0;
		OptionChainRuleSignals.VolatilitySupplyExhaustionSignal signal = new OptionChainRuleSignals.VolatilitySupplyExhaustionSignal();
		signal.setExhaustionScore(exhaustionScore);
		signal.setState(exhaustionScore > 0.5 ? "POTENTIAL_VOL_BREAKOUT" : "SUPPLY_INTACT");
		signal.setStrength(exhaustionScore);
		return signal;
	}

	private double findClosestStrike(List<OptionChainData> chain, double spotPrice) {
		return chain.stream()
				.min(Comparator.comparingDouble(o -> Math.abs(o.getStrike_price() - spotPrice)))
				.map(OptionChainData::getStrike_price)
				.orElse(spotPrice);
	}
}
