package com.tradebot.upstox.context;

import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

/**
 * VIX regime from level and day-over-day change. Plan: LOW_VOL &lt; 13,
 * 13–20 NORMAL, &gt; 20 HIGH_VOL; SPIKING when Δ &gt; 10%.
 */
@Service
public class VixRegimeServiceImpl implements VixRegimeService {

    private static final double LOW_THRESHOLD = 13.0;
    private static final double HIGH_THRESHOLD = 20.0;
    private static final double SPIKE_PCT = 0.10;

    @Override
    public String determineRegime(double vix, @Nullable Double previousVix) {
        if (previousVix != null && previousVix > 0 && Math.abs((vix - previousVix) / previousVix) > SPIKE_PCT) {
            return "SPIKING";
        }
        if (vix < LOW_THRESHOLD) {
            return "LOW_VOL";
        }
        if (vix > HIGH_THRESHOLD) {
            return "HIGH_VOL";
        }
        return "NORMAL";
    }
}
