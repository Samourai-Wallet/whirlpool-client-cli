package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunListPools {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliWalletService cliWalletService;
  private CliConfig cliConfig;

  public RunListPools(CliWalletService cliWalletService, CliConfig cliConfig) {
    this.cliWalletService = cliWalletService;
    this.cliConfig = cliConfig;
  }

  public void run() throws Exception {
    Pools pools = cliWalletService.listPools(cliConfig);

    // show available pools
    String lineFormat = "| %15s | %6s | %15s | %14s | %12s | %15s | %23s |\n";
    StringBuilder sb = new StringBuilder();
    sb.append(
        String.format(
            lineFormat,
            "POOL ID",
            "DENOM.",
            "STATUS",
            "USERS",
            "LAST MIX",
            "ANONYMITY SET",
            "MUSTMIX BALANCE"));
    sb.append(
        String.format(
            lineFormat, "", "(btc)", "", "(confir/reg)", "", "(target/min)", "min-max (sat)"));
    for (Pool pool : pools.getPools()) {
      sb.append(
          String.format(
              lineFormat,
              pool.getPoolId(),
              ClientUtils.satToBtc(pool.getDenomination()),
              pool.getMixStatus(),
              pool.getNbConfirmed() + " / " + pool.getNbRegistered(),
              pool.getElapsedTime() / 1000 + "s",
              pool.getMixAnonymitySet() + " / " + pool.getMinAnonymitySet(),
              pool.getMustMixBalanceMin() + " - " + pool.getMustMixBalanceMax()));
    }
    log.info("\n" + sb.toString());
  }
}
