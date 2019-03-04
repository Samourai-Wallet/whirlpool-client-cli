package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.beans.Encrypted;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
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
    log.info(
        "⣿ CLI INITIALIZATION ⣿ This will intialize CLI and connect it to your existing Samourai Wallet.");
    String seedPassphrase = CliUtils.readUserInput("Seed passphrase", true);
    String seedWords = CliUtils.readUserInput("Seed words", true);

    if (!cliWalletService.checkSeedValid(seedWords, seedPassphrase)) {
      throw new NotifiableException("Your seed is invalid. Please try again.");
    }

    // encrypt seedWords with seedPassphrase
    Encrypted encryptedSeedWords = cliWalletService.encryptSeedWords(seedWords, seedPassphrase);

    // init
    String apiKey = cliConfigService.initialize(encryptedSeedWords);

    log.info(
        "⣿ API KEY GENERATED ⣿ An API key has been generated, take note of it. You will need it to connect remotely from GUI (with --listen). Your API key is: "
            + apiKey);
  }
}
