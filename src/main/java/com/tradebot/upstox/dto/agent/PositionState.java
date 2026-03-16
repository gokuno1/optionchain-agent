package com.tradebot.upstox.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot of open position and daily trade state for the agent prompt.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PositionState {

    private boolean hasOpenPosition;
    private String instrument;
    private double entryPrice;
    private double currentPnlPct;
    private int holdingMinutes;
    private double dayPnl;
    private int tradesTaken;
    private int tradesLimit;
}
