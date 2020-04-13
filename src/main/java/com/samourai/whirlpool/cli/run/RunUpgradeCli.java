package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliConfigService;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunUpgradeCli {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliConfig cliConfig;
  private CliConfigService cliConfigService;

  public RunUpgradeCli(CliConfig cliConfig, CliConfigService cliConfigService) {
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
  }

  public void run(int localVersion) throws Exception {
    // run upgrades
    if (localVersion < CliConfigService.CLI_VERSION_4) {
      upgradeV4();
    }
  }

  public void upgradeV4() throws Exception {
    log.info(" - Upgrading to: V4");

    // set cli.mix.clients=5 when missing
    if (cliConfig.getMix().getClients() == 0) {
      Properties props = cliConfigService.loadProperties();
      props.put(CliConfigService.KEY_MIX_CLIENTS, "5");
      cliConfigService.saveProperties(props);
    }
  }
}
