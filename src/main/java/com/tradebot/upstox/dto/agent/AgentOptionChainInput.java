package com.tradebot.upstox.dto.agent;

import com.tradebot.upstox.dto.optionchain.OptionChainData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Raw option chain data for the agent pipeline: filtered chain, spot, VIX, expiry.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentOptionChainInput {

    private List<OptionChainData> filteredChain;
    private double spotPrice;
    private double vix;
    private String expiryDate;
}
