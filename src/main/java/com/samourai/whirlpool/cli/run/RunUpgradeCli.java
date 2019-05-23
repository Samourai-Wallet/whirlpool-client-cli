package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliConfigService;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunUpgradeCli {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int CLI_V1 = 1;

  private CliConfig cliConfig;
  private CliConfigService cliConfigService;

  public RunUpgradeCli(CliConfig cliConfig, CliConfigService cliConfigService) {
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
  }

  public void run(int lastVersion) throws Exception {
    // run upgrades
    if (lastVersion < CLI_V1) {
      upgradeV1();
    }
  }

  public void upgradeV1() throws Exception {
    log.info(" - Upgrading to: V1");
    // set cli.seedAppendPassphrase=true
    cliConfigService.setVersionCurrent();
  }
}
