package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.beans.WhirlpoolPairingPayload;
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
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ CLI INITIALIZATION");
    log.info("⣿ This will intialize CLI and connect it to your existing Samourai Wallet.");
    log.info("⣿ ");

    // pairing payload
    log.info(
        "⣿ Get your pairing payload in Samourai Wallet, go to 'Settings/Transactions/Experimental'");
    log.info("⣿ • Paste your pairing payload here:");
    String pairingPayload = CliUtils.readUserInputRequired("Pairing payload?", false);
    log.info("⣿ ");
    WhirlpoolPairingPayload pairing = cliConfigService.parsePairingPayload(pairingPayload);

    // Tor
    boolean tor;
    if (pairing.getDojo() != null) {
      // dojo => Tor enabled
      log.info("⣿ Pairing with Dojo => Tor enabled.");
      tor = true;
    } else {
      // samourai backend => Tor optional
      log.info("⣿ • Enable Tor? (you can change this later)");
      String torStr =
          CliUtils.readUserInputRequired(
              "Enable Tor? (y/n)", false, new String[] {"y", "n", "Y", "N"});
      tor = torStr.toLowerCase().equals("y");
      log.info("⣿ ");
    }

    // init
    String apiKey = cliConfigService.initialize(pairing, tor, null);

    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ API KEY GENERATED");
    log.info("⣿ Take note of your API Key, to connect remotely from GUI or API.");
    log.info("⣿ Your API key is: " + apiKey);
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("");
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ INITIALIZATION SUCCESS");
    log.info("⣿ Please restart CLI.");
    log.info(CliUtils.LOG_SEPARATOR);
  }
}
