package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunUpgradeCli {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int CLI_V4 = 4;

  private CliConfig cliConfig;
  private CliConfigService cliConfigService;

  public RunUpgradeCli(CliConfig cliConfig, CliConfigService cliConfigService) {
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
  }

  public void run(int lastVersion) throws Exception {
    // run upgrades
    if (lastVersion < CLI_V4) {
      upgradeV4();
    }
  }

  public void upgradeV4() throws NotifiableException {
    log.info("Upgrading CLI to: V4");
    // set cli.seedAppendPassphrase=true
    cliConfigService.initialize(cliConfig.getSeed(), true, cliConfig.getServer());
  }
}
