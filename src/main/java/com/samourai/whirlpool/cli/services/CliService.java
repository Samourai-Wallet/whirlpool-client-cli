package com.samourai.whirlpool.cli.services;

import com.samourai.whirlpool.cli.ApplicationArgs;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.beans.CliResult;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.run.CliStatusOrchestrator;
import com.samourai.whirlpool.cli.run.RunCliCommand;
import com.samourai.whirlpool.cli.run.RunCliInit;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CliService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int CLI_STATUS_DELAY = 5000;

  private ApplicationArgs appArgs;
  private CliConfig cliConfig;
  private CliConfigService cliConfigService;
  private CliWalletService cliWalletService;
  private WalletAggregateService walletAggregateService;
  private CliTorClientService cliTorClientService;
  private CliStatusOrchestrator cliStatusOrchestrator;

  public CliService(
      ApplicationArgs appArgs,
      CliConfig cliConfig,
      CliConfigService cliConfigService,
      CliWalletService cliWalletService,
      WalletAggregateService walletAggregateService,
      CliTorClientService cliTorClientService) {
    this.appArgs = appArgs;
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
    this.cliWalletService = cliWalletService;
    this.walletAggregateService = walletAggregateService;
    this.cliTorClientService = cliTorClientService;
    this.cliStatusOrchestrator = null;

    setup();
  }

  private void setup() {
    // properties were set on CliConfig => override CliConfig with cli args
    appArgs.override(cliConfig);

    // setup proxy
    Optional<CliProxy> cliProxyOptional = cliConfig.getCliProxy();
    if (cliProxyOptional.isPresent()) {
      CliProxy cliProxy = cliProxyOptional.get();
      CliUtils.useProxy(cliProxy);
    }
  }

  private File computeDirLockFile() throws NotifiableException {
    String path = "whirlpool-cli.lock";
    return CliUtils.computeFile(path);
  }

  private FileLock lockDirectory() throws Exception {
    File dirLockFile = computeDirLockFile();
    dirLockFile.deleteOnExit();
    String lockErrorMsg =
        "Another Whirlpool instance seems already running in current directory. Otherwise, you may manually delete "
            + dirLockFile.getAbsolutePath();
    FileLock dirFileLock = ClientUtils.lockFile(dirLockFile, lockErrorMsg);
    return dirFileLock;
  }

  public CliResult run(boolean listen) throws Exception {
    String[] args = appArgs.getApplicationArguments().getSourceArgs();

    log.info("------------ whirlpool-client-cli starting ------------");
    log.info(
        "Running whirlpool-client-cli on java {}... {}",
        System.getProperty("java.version"),
        Arrays.toString(args));

    // get dir lock
    FileLock dirFileLock = lockDirectory();
    try {
      // log config
      if (log.isDebugEnabled()) {
        for (Map.Entry<String, String> entry : cliConfig.getConfigInfo().entrySet()) {
          log.debug("[cliConfig/" + entry.getKey() + "] " + entry.getValue());
        }
      }

      // connect Tor
      cliTorClientService.connect();

      // initialize bitcoinj context
      NetworkParameters params = cliConfig.getServer().getParams();
      new Context(params);

      // check init
      if (appArgs.isInit() || (cliConfigService.isCliStatusNotInitialized() && !listen)) {
        new RunCliInit(appArgs, cliConfigService, cliWalletService).run();
        return CliResult.RESTART;
      }

      // check cli initialized
      if (cliConfigService.isCliStatusNotInitialized()) {
        // not initialized
        if (log.isDebugEnabled()) {
          log.debug("CliStatus=" + cliConfigService.getCliStatus());
        }

        // keep cli running for remote initialization
        log.warn(CliUtils.LOG_SEPARATOR);
        log.warn("⣿ INITIALIZATION REQUIRED");
        log.warn("⣿ Please start GUI to initialize CLI.");
        log.warn("⣿ Or initialize with --init");
        log.warn(CliUtils.LOG_SEPARATOR);
        keepRunning();
        return CliResult.KEEP_RUNNING;
      }

      // check upgrade
      boolean shouldRestart = cliConfigService.checkUpgrade();
      if (shouldRestart) {
        log.warn(CliUtils.LOG_SEPARATOR);
        log.warn("⣿ UPGRADE SUCCESS");
        log.warn("⣿ Restarting CLI...");
        log.warn(CliUtils.LOG_SEPARATOR);
        return CliResult.RESTART;
      }

      if (listen) {
        String info = "API is listening on https://127.0.0.1:" + cliConfig.getApi().getPort();
        if (cliConfig.getApi().isHttpEnable()) {
          info += " and http://127.0.0.1:" + cliConfig.getApi().getHttpPort();
        }
        log.info(info);
      }

      if (!appArgs.isAuthenticate()
          && listen
          && !RunCliCommand.hasCommandToRun(appArgs, cliConfig)) {
        // no passphrase but listening => keep listening
        log.info(CliUtils.LOG_SEPARATOR);
        log.info("⣿ AUTHENTICATION REQUIRED");
        log.info("⣿ Whirlpool wallet is CLOSED.");
        log.info("⣿ Please start GUI to authenticate and start mixing.");
        log.info("⣿ Or authenticate with --authenticate");
        log.info(CliUtils.LOG_SEPARATOR);
        keepRunning();
        return CliResult.KEEP_RUNNING;
      }

      // authenticate to open wallet when passphrase providen through arguments
      String seedPassphrase = authenticate();

      // we may have authenticated from API in the meantime...
      CliWallet cliWallet =
          cliWalletService.hasSessionWallet()
              ? cliWalletService.getSessionWallet()
              : cliWalletService.openWallet(seedPassphrase);
      log.info(CliUtils.LOG_SEPARATOR);
      log.info("⣿ AUTHENTICATION SUCCESS");
      log.info("⣿ Whirlpool is starting...");
      log.info(CliUtils.LOG_SEPARATOR);

      if (RunCliCommand.hasCommandToRun(appArgs, cliConfig)) {
        // execute specific command
        new RunCliCommand(appArgs, cliWalletService, walletAggregateService).run();
        return CliResult.EXIT_SUCCESS;
      } else {
        // start wallet
        cliWallet.start();
        keepRunning();
        return CliResult.KEEP_RUNNING;
      }
    } finally {
      ClientUtils.unlockFile(dirFileLock);
      computeDirLockFile().delete();
    }
  }

  private void keepRunning() {
    // disable statusOrchestrator when redirecting output
    if (CliUtils.hasConsole()) {
      // log status
      this.cliStatusOrchestrator =
          new CliStatusOrchestrator(CLI_STATUS_DELAY, cliWalletService, cliConfig);
      this.cliStatusOrchestrator.start(true);
    }
  }

  private String authenticate() {
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ AUTHENTICATION REQUIRED");
    log.info("⣿ Whirlpool wallet is CLOSED.");
    log.info("⣿ • Please type your seed passphrase to authenticate and start mixing.");
    return CliUtils.readUserInputRequired("Seed passphrase?", true);
  }

  public void shutdown() {
    log.info("------------ whirlpool-client-cli ending ------------");
    if (log.isDebugEnabled()) {
      log.debug("shutdown");
    }

    // stop cliStatusOrchestrator
    if (cliStatusOrchestrator != null) {
      cliStatusOrchestrator.stop();
    }

    // stop cliWallet
    try {
      CliWallet cliWallet =
          cliWalletService != null && cliWalletService.hasSessionWallet()
              ? cliWalletService.getSessionWallet()
              : null;
      if (cliWallet != null && cliWallet.getMixingState().isStarted()) {
        cliWallet.stop();
      }
    } catch (NoSessionWalletException e) {
    }

    // disconnect Tor
    if (cliTorClientService != null) {
      cliTorClientService.shutdown();
    }
  }
}
