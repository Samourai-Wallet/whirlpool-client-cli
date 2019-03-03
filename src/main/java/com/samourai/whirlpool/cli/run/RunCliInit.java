package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunCliInit {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private ApplicationArgs appArgs;
  private CliConfigService cliConfigService;
  private CliWalletService cliWalletService;

  public RunCliInit(
      ApplicationArgs appArgs,
      CliConfigService cliConfigService,
      CliWalletService cliWalletService) {
    this.appArgs = appArgs;
    this.cliConfigService = cliConfigService;
    this.cliWalletService = cliWalletService;
  }

  public void run() throws Exception {
    String seedPassphrase = CliUtils.readUserInput("Seed passphrase?", true);
    String seedWords = CliUtils.readUserInput("Seed words?", true);

    // encrypt seedWords with seedPassphrase
    String encryptedSeedWords = cliWalletService.encryptSeedWords(seedWords, seedPassphrase);

    // init
    cliConfigService.initialize(encryptedSeedWords);
  }
}
