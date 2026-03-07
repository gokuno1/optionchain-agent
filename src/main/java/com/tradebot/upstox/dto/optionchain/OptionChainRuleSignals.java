package com.tradebot.upstox.dto.optionchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionChainRuleSignals {
	private OiVelocitySignal oiVelocity;
	private OpeningVsClosingSignal openingVsClosing;
	private DealerHedgePressureSignal dealerHedgePressure;
	private OiCenterOfGravitySignal oiCenterOfGravity;
	private ConvexityAsymmetrySignal convexityAsymmetry;
	private LiquidityVoidSignal liquidityVoid;
	private StrikeRollSignal strikeRoll;
	private CalendarConflictSignal calendarConflict;
	private SkewResponsivenessSignal skewResponsiveness;
	private DealerNeutralCorridorSignal dealerNeutralCorridor;
	private IntrinsicDominanceSignal intrinsicDominance;
	private StrikeDefenseFailureSignal strikeDefenseFailure;
	private SyntheticForwardDistortionSignal syntheticForwardDistortion;
	private ParticipantTrapSignal participantTrap;
	private VolatilitySupplyExhaustionSignal volatilitySupplyExhaustion;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class StrikeBand {
		private double from;
		private double to;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OiVelocitySignal {
		private String state;
		private double strength;
		private String priceMove;
		private double callDeltaOiPct;
		private double putDeltaOiPct;
		private StrikeBand topResistanceBand;
		private StrikeBand topSupportBand;
		private boolean atmContractionActive;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OpeningVsClosingSignal {
		private String state;
		private double strength;
		private double positionalVolumeShare;
		private double churnVolumeShare;
		private StrikeBand topMagnetBand;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class DealerHedgePressureSignal {
		private String state;
		private double strength;
		private double netPublicCallDelta;
		private double netPublicPutDelta;
		private double netDealerDeltaSensitivity;
		private double moneynessWeightedCenter;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OiCenterOfGravitySignal {
		private String state;
		private double strength;
		private double cogStrike;
		private Double prevCogStrike;
		private double driftPoints;
		private double distanceFromSpot;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ConvexityAsymmetrySignal {
		private String state;
		private double strength;
		private double atmCallOiShare;
		private double atmPutOiShare;
		private double otmCallOiShare;
		private double otmPutOiShare;
		private StrikeBand dominantWingBand;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class LiquidityVoidSignal {
		private String state;
		private double strength;
		private StrikeBand voidAboveSpot;
		private StrikeBand voidBelowSpot;
		private StrikeBand topHighOiCluster;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class StrikeRollSignal {
		private String state;
		private double strength;
		private double fromStrike;
		private double toStrike;
		private String side;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class CalendarConflictSignal {
		private String state;
		private double strength;
		private Double nearPcr;
		private Double farPcr;
		private String nearExpiry;
		private String farExpiry;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SkewResponsivenessSignal {
		private String state;
		private double strength;
		private String priceMove;
		private double callSkewSlope;
		private double putSkewSlope;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class DealerNeutralCorridorSignal {
		private String state;
		private double strength;
		private double lowerBound;
		private double upperBound;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class IntrinsicDominanceSignal {
		private String state;
		private double strength;
		private double intrinsicOiShare;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class StrikeDefenseFailureSignal {
		private String state;
		private double strength;
		private double failedStrike;
		private String side;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SyntheticForwardDistortionSignal {
		private String state;
		private double strength;
		private double avgDistortion;
		private double maxDistortion;
		private double maxDistortionStrike;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ParticipantTrapSignal {
		private String state;
		private double strength;
		private StrikeBand retailShortZone;
		private StrikeBand institutionalWingZone;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class VolatilitySupplyExhaustionSignal {
		private String state;
		private double strength;
		private double exhaustionScore;
	}
}
