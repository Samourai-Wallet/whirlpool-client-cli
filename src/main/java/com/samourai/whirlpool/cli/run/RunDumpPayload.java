package com.samourai.whirlpool.cli.run;

import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunDumpPayload {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliWalletService cliWalletService;

  public RunDumpPayload(CliWalletService cliWalletService) {
    this.cliWalletService = cliWalletService;
  }

  public void run() throws Exception {
    String payload = cliWalletService.computePairingPayload();
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ DUMP-PAYLOAD");
    log.info("⣿ Pairing-payload of your current wallet:");
    log.info("⣿ " + payload);
    log.info(CliUtils.LOG_SEPARATOR);
  }
}
