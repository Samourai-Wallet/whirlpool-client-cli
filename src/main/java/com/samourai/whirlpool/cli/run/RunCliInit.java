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
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ CLI INITIALIZATION");
    log.info("⣿ This will intialize CLI and connect it to your existing Samourai Wallet.");
    log.info("⣿ ");
    log.info("⣿ Your passphrase is used to encrypt your wallet seed, but won't be stored.");
    log.info("⣿ Your passphrase is required for each whirlpool startup.");
    log.info("⣿ • Please type your seed passphrase");
    String seedPassphrase = CliUtils.readUserInputRequired("Seed passphrase?", true);
    log.info("⣿ ");

    log.info("⣿ Your seed words will be encrypted in ./" + CliConfigService.CLI_CONFIG_FILENAME);
    log.info("⣿ whirlpool will never ask again for it.");
    log.info("⣿ • Please type your seed words (12 words separated with space, no quotes)");
    String seedWords = CliUtils.readUserInputRequired("Seed words?", true);
    log.info("⣿ ");
    log.info("⣿ • Encrypting seed...");

    if (!cliWalletService.checkSeedValid(seedWords, seedPassphrase)) {
      throw new NotifiableException("Your seed is invalid. Please try again.");
    }

    // encrypt seedWords with seedPassphrase
    Encrypted encryptedSeedWords = cliWalletService.encryptSeedWords(seedWords, seedPassphrase);

    // init
    String apiKey = cliConfigService.initialize(encryptedSeedWords);

    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ API KEY GENERATED");
    log.info("⣿ Please take note of your API Key.");
    log.info("⣿ You will need it to connect remotely from GUI.");
    log.info("⣿ Your API key is: " + apiKey);
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("");
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ RESTART REQUIRED");
    log.info("⣿ Wallet inizialization success.");
    log.info("⣿ Please restart CLI.");
    log.info(CliUtils.LOG_SEPARATOR);
  }
}
