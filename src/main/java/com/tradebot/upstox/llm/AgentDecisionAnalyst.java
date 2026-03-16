package com.tradebot.upstox.llm;

import com.tradebot.upstox.dto.agent.AgentDecision;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LLM interface for taking DistilledFeatures (23 signals + VIX regime + position state)
 * and returning a structured BUY_CE / BUY_PE / HOLD decision.
 */
public interface AgentDecisionAnalyst {

    @SystemMessage("""
            You are an expert NIFTY 50 intraday options trader. You receive pre-computed
            market microstructure signals and must decide a single action: BUY_CE, BUY_PE, or HOLD.

            RULES:
            - Only recommend BUY_CE or BUY_PE when your confidence exceeds 0.85
            - HOLD is always safe. When in doubt, HOLD
            - Stop loss must be within 1-3% of the entry price midpoint
            - Target must be within 1.5-5% of the entry price midpoint
            - Reward-to-risk ratio must exceed 1.5
            - On EXPIRY_DAY: weight CHARM_PRESSURE and PINNING_PRESSURE heavily
            - Use GEX, OI_VELOCITY, STRIKE_DEFENSE_FAILURE, DEALER_HEDGE_PRESSURE, VANNA_FLOW
              as your primary directional cues; use other signals (e.g. LIQUIDITY_VOID, PARTICIPANT_TRAP,
              OI_ENTROPY, SKEW_RESPONSIVENESS, SMART_MONEY_DIVERGENCE) as confirmation/context.

            OUTPUT: Respond with ONLY this JSON object, no markdown, no extra text:
            JSON schema: {"direction_bias":"bullish|bearish|neutral","volatility_expectation":"low|moderate|high","key_levels":[number],"trade_strike":"e.g. 24500 CE or NO_TRADE","instrument_type":"CE|PE or empty when NO_TRADE","strike_price":"number or 0 when NO_TRADE","target_percent":"number","stop_percent":"number","reasoning":"short explanation","confidence":"low|medium|high"}
            """)
    @UserMessage("""
            Underlying index: {index}
            Expiry: {expiry}

            Distilled features JSON (spot, VIX, vixRegime, 23 microstructure signals, position state):
            {featuresJson}
            """)
    AgentDecision decide(@V("index") String index,
                         @V("expiry") String expiry,
                         @V("featuresJson") String featuresJson);
}

