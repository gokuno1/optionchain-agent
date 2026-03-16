package com.tradebot.upstox.context;

import javax.annotation.Nullable;

/**
 * Determines VIX regime from current and optional previous VIX level.
 * Regimes: LOW_VOL, NORMAL, HIGH_VOL, SPIKING.
 */
public interface VixRegimeService {

    /**
     * @param vix         current India VIX value
     * @param previousVix previous cycle VIX (null if first run)
     * @return one of LOW_VOL, NORMAL, HIGH_VOL, SPIKING
     */
    String determineRegime(double vix, @Nullable Double previousVix);
}
