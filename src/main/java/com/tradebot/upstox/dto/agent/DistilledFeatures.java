package com.tradebot.upstox.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Distilled feature set for the intraday agent: 23 microstructure signals,
 * VIX regime, composite agreement, and change-detector score. No candles/TA.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistilledFeatures {

    private double spotPrice;
    private double vix;
    private String vixRegime;
    private List<OptionSignal> signals = new ArrayList<>();
}
