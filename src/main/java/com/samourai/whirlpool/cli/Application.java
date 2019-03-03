package com.samourai.whirlpool.cli;

import com.samourai.stomp.client.JavaStompClient;
import com.samourai.tor.client.JavaTorClient;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.run.RunCliCommand;
import com.samourai.whirlpool.cli.run.RunUpgradeCli;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.LogbackUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;

/** Command-line client. */
@SpringBootApplication
@ServletComponentScan(value = "com.samourai.whirlpool.cli.config.filters")
public class Application implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String INDEX_CLI_VERSION = "cliVersion";

  private static final int CLI_VERSION = 3;

  private static Integer listenPort;
  private static ConfigurableApplicationContext applicationContext;

  @Autowired private ApplicationArgs appArgs;
  @Autowired private CliConfig cliConfig;
  @Autowired private CliConfigService cliConfigService;
  @Autowired private CliWalletService cliWalletService;
  @Autowired private PushTxService pushTxService;
  @Autowired private JavaStompClient stompClient;
  @Autowired private Bech32UtilGeneric bech32Util;
  @Autowired private WalletAggregateService walletAggregateService;

  public static void main(String... args) {
    // start REST api if --listen
    listenPort = ApplicationArgs.getMainListen(args);
    WebApplicationType wat =
        listenPort != null ? WebApplicationType.SERVLET : WebApplicationType.NONE;
    if (listenPort != null) {
      System.setProperty("server.port", Integer.toString(listenPort));
    }

    // run
    applicationContext = new SpringApplicationBuilder(Application.class).web(wat).run(args);

    // quit?
    applicationContext.close();
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info("------------ whirlpool-client-cli starting ------------");
    log.info(
        "Running whirlpool-client {} on java {}",
        Arrays.toString(args.getSourceArgs()),
        System.getProperty("java.version"));
    if (log.isDebugEnabled()) {
      for (Map.Entry<String, String> entry : cliConfig.getConfigInfo().entrySet()) {
        log.debug("config/initial: " + entry.getKey() + ": " + entry.getValue());
      }
    }

    // enable debug logs with --debug
    setDebug(appArgs.isDebug(), appArgs.isDebugClient());

    Optional<JavaTorClient> torClient = Optional.empty();

    if (log.isDebugEnabled()) {
      for (Map.Entry<String, String> entry : cliConfig.getConfigInfo().entrySet()) {
        log.debug("config/override: " + entry.getKey() + ": " + entry.getValue());
      }
      log.debug("config/initial: listen: " + (listenPort != null ? listenPort : "false"));
    }

    WhirlpoolWallet whirlpoolWallet = null;
    try {
      // initialize bitcoinj context
      NetworkParameters params = cliConfig.getServer().getParams();
      new Context(params);

      // check pushTxService
      if (!pushTxService.testConnectivity()) {
        throw new NotifiableException("Unable to connect to pushTxService");
      }

      // check cli initialized
      if (cliConfigService.isCliStatusReady()) {

        // init wallet
        whirlpoolWallet =
            cliWalletService.openWallet(appArgs.getSeedWords(), appArgs.getSeedPassphrase());

        // check whirlpool connectivity
        if (!cliWalletService.testConnectivity()) {
          throw new NotifiableException("Unable to connect to Whirlpool server");
        }

        // check upgrade wallet
        checkUpgradeWallet();

        if (RunCliCommand.hasCommandToRun(appArgs)) {
          // WhirlpoolClient instanciation
          WhirlpoolClientConfig whirlpoolClientConfig = cliConfig.computeWhirlpoolWalletConfig();
          WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(whirlpoolClientConfig);

          // execute specific command
          new RunCliCommand(
                  appArgs,
                  whirlpoolClient,
                  whirlpoolClientConfig,
                  cliWalletService,
                  bech32Util,
                  walletAggregateService)
              .run();
        } else {
          // start wallet
          whirlpoolWallet.start();

          // keep cli running
          keepRunning();
        }
      } else {
        // cli not initialized
        log.warn("*** CLI config not initialized yet ***");
        if (log.isDebugEnabled()) {
          log.debug("CliStatus=" + cliConfigService.getCliStatus());
        }

        if (listenPort != null) {
          log.info("Listening for remote initialization...");

          // keep cli running for remote initialization
          keepRunning();
        }
      }
    } catch (NotifiableException e) {
      log.error("*** ERROR *** " + e.getMessage());
    } catch (IllegalArgumentException e) {
      log.info("Invalid arguments: " + e.getMessage());
    } catch (Exception e) {
      log.error("", e);
    }

    log.info("------------ whirlpool-client-cli ending ------------");

    // stop cliWallet
    if (whirlpoolWallet != null && whirlpoolWallet.isStarted()) {
      whirlpoolWallet.stop();
    }

    // disconnect
    if (torClient.isPresent()) {
      torClient.get().disconnect();
    }
    stompClient.disconnect();
  }

  private void keepRunning() {
    while (true) {
      try {
        synchronized (this) {
          wait();
        }
      } catch (InterruptedException e) {
      }
    }
  }

  private void checkUpgradeWallet() throws Exception {
    IIndexHandler cliVersionHandler =
        cliWalletService.getFileIndexHandler().getIndexHandler(INDEX_CLI_VERSION, CLI_VERSION);
    int lastVersion = cliVersionHandler.get();

    if (lastVersion == CLI_VERSION) {
      // up to date
      if (log.isDebugEnabled()) {
        log.debug("cli wallet is up to date: " + CLI_VERSION);
      }
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug(" • Upgrading cli wallet: " + lastVersion + " -> " + CLI_VERSION);
    }
    new RunUpgradeCli(cliWalletService).run(lastVersion);

    // set new version
    cliVersionHandler.set(CLI_VERSION);
  }

  private void setDebug(boolean isDebug, boolean isDebugClient) {
    if (isDebug) {
      LogbackUtils.setLogLevel("com.samourai", Level.DEBUG.toString());
      // Utils.setLoggerDebug("org.springframework.security");
    } else {
      LogbackUtils.setLogLevel("org.silvertunnel_ng.netlib", Level.ERROR.toString());
    }

    if (isDebugClient) {
      LogbackUtils.setLogLevel("com.samourai.whirlpool.client", Level.DEBUG.toString());
      LogbackUtils.setLogLevel("com.samourai.stomp.client", Level.DEBUG.toString());
    } else {
      LogbackUtils.setLogLevel("com.samourai.whirlpool.client", Level.INFO.toString());
      LogbackUtils.setLogLevel("com.samourai.stomp.client", Level.INFO.toString());
    }

    if (isDebug) {
      LogbackUtils.setLogLevel("com.samourai.whirlpool.client.wallet", Level.DEBUG.toString());
    }

    // skip noisy logs
    LogbackUtils.setLogLevel(
        "org.silvertunnel_ng.netlib.layer.tor.directory.RouterParserCallable",
        Level.ERROR.toString());
    LogbackUtils.setLogLevel(
        "org.silvertunnel_ng.netlib.layer.tor.directory.Directory", Level.ERROR.toString());
    LogbackUtils.setLogLevel(
        "org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter",
        Level.INFO.toString());
  }
}
