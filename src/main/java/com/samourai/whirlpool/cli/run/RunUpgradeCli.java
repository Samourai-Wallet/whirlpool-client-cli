package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.services.CliWalletService;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunUpgradeCli {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int CLI_V1 = 1;
  private static final int CLI_V2 = 2;
  private static final int CLI_V3 = 3;

  private CliWalletService cliWalletService;

  public RunUpgradeCli(CliWalletService cliWalletService) {
    this.cliWalletService = cliWalletService;
  }

  public void run(int lastVersion) throws Exception {
    // run upgrades
  }
}
