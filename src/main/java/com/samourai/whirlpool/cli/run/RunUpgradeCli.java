package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliConfigService;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunUpgradeCli {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int CLI_V1 = 1;
  private static final int CLI_V3 = 3;

  private CliConfig cliConfig;
  private CliConfigService cliConfigService;

  public RunUpgradeCli(CliConfig cliConfig, CliConfigService cliConfigService) {
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
  }

  public void run(int lastVersion) throws Exception {
    // run upgrades
    if (lastVersion < CLI_V3) {
      upgradeV3();
    }
  }

  public void upgradeV3() throws Exception {
    log.info(" - Upgrading to: V3");

    // unset mix.clients=
    Properties props = cliConfigService.loadProperties();
    props.put(CliConfigService.KEY_MIX_CLIENTS, "");
    cliConfigService.saveProperties(props);
  }
}
