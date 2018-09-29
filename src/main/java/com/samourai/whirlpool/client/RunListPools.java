package com.samourai.whirlpool.client;

import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunListPools {
    private static final Logger log = LoggerFactory.getLogger(RunListPools.class);

    public RunListPools() {}

    public void run(Pools pools) {
        // show available pools
        String lineFormat = "| %15s | %6s | %15s | %22s | %12s | %15s | %13s |\n";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(lineFormat, "POOL ID", "DENOM.", "STATUS", "USERS", "ELAPSED TIME", "ANONYMITY SET", "MINER FEE"));
        sb.append(String.format(lineFormat, "", "(btc)", "", "(confirmed/registered)", "", "(target/min)", "min-max (sat)"));
        for (Pool pool : pools.getPools()) {
            sb.append(String.format(lineFormat, pool.getPoolId(),  satToBtc(pool.getDenomination()), pool.getMixStatus(), pool.getMixNbConfirmed() + " / " + pool.getNbRegistered(), pool.getElapsedTime()/1000 + "s", pool.getMixAnonymitySet() + " / " + pool.getMinAnonymitySet(), pool.getMinerFeeMin() + " - " + pool.getMinerFeeMax()));
        }
        log.info("\n" + sb.toString());
    }

    private double satToBtc(long sat) {
        return sat / 100000000.0;
    }
}
