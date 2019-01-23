package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunListPools {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private WhirlpoolClient whirlpoolClient;

  public RunListPools(WhirlpoolClient whirlpoolClient) {
    this.whirlpoolClient = whirlpoolClient;
  }

  public void run() throws Exception {
    Pools pools = whirlpoolClient.fetchPools();

    // show available pools
    String lineFormat = "| %15s | %6s | %15s | %22s | %12s | %15s | %13s |\n";
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            lineFormat,
            "POOL ID",
            "DENOM.",
            "STATUS",
            "USERS",
            "ELAPSED TIME",
            "ANONYMITY SET",
            "MINER FEE"));
    sb.append(
        String.format(
            lineFormat,
            "",
            "(btc)",
            "",
            "(confirmed/registered)",
            "",
            "(target/min)",
            "min-max (sat)"));
    for (Pool pool : pools.getPools()) {
      sb.append(
          String.format(
              lineFormat,
              pool.getPoolId(),
              ClientUtils.satToBtc(pool.getDenomination()),
              pool.getMixStatus(),
              pool.getMixNbConfirmed() + " / " + pool.getNbRegistered(),
              pool.getElapsedTime() / 1000 + "s",
              pool.getMixAnonymitySet() + " / " + pool.getMinAnonymitySet(),
              pool.getMinerFeeMin() + " - " + pool.getMinerFeeMax()));
    }
    log.info("\n" + sb.toString());
  }
}
