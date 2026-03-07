package com.tradebot.upstox.model;

import java.math.BigInteger;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * In-memory POJO for option chain analysis (no persistence).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionChainAnalysisData {
	private double avgPutDelta;
	private double avgCallDelta;
	private double avgPutIv;
	private double avgCallIv;
	private double avgPutOtmIv;
	private double avgCallOtmIv;
	private double putCallRatio;
	private double spotPrice;
	private double vixLtp;
	private double strikePrice;
	private String putCallParity;
	private Date createdDate;
	private BigInteger callVolume;
	private BigInteger putVolume;
	private BigInteger callChangeOI;
	private BigInteger callTotalOI;
	private BigInteger putChangeOI;
	private BigInteger putTotalOI;
	private double avgPutTheta;
	private double avgCallTheta;
}
