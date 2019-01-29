package com.samourai.whirlpool.cli;

import com.samourai.api.client.SamouraiApi;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.tor.client.JavaTorClient;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.run.RunCliCommand;
import com.samourai.whirlpool.cli.run.RunListPools;
import com.samourai.whirlpool.cli.run.RunUpgradeCli;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.LogbackUtils;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
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

/** Command-line client. */
@SpringBootApplication
@ServletComponentScan(value = "com.samourai.whirlpool.cli.config.filters")
public class Application implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int CLI_VERSION = 3;

  private static final String INDEX_CLI_VERSION = "cliVersion";

  private static boolean isListen;

  @Autowired private CliConfig cliConfig;
  @Autowired private CliWalletService cliWalletService;
  @Autowired private WhirlpoolClient whirlpoolClient;
  @Autowired private PushTxService pushTxService;
  @Autowired private JavaStompClient stompClient;
  @Autowired private SamouraiApi samouraiApi;
  @Autowired private WhirlpoolClientConfig whirlpoolClientConfig;
  @Autowired private Bech32UtilGeneric bech32Util;
  @Autowired private WalletAggregateService walletAggregateService;
  @Autowired private CliTorClientService torClientService;
  @Autowired private Tx0Service tx0Service;

  public static void main(String... args) {
    // start REST api if --listen
    isListen = ApplicationArgs.isMainListen(args);
    WebApplicationType wat = isListen ? WebApplicationType.SERVLET : WebApplicationType.NONE;
    new SpringApplicationBuilder(Application.class).web(wat).run(args);
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info("------------ whirlpool-client ------------");
    log.info(
        "Running whirlpool-client {} on java {}",
        Arrays.toString(args.getSourceArgs()),
        System.getProperty("java.version"));
    if (log.isDebugEnabled()) {
      for (Map.Entry<String, String> entry : cliConfig.getConfigInfo().entrySet()) {
        log.debug("config/initial: " + entry.getKey() + ": " + entry.getValue());
      }
    }

    ApplicationArgs appArgs = new ApplicationArgs(args);

    // override configuration file with cli args
    appArgs.override(cliConfig);

    // enable debug logs with --debug
    if (cliConfig.isDebug()) {
      LogbackUtils.setLogLevel("com.samourai", Level.DEBUG.toString());
    } else {
      LogbackUtils.setLogLevel("org.silvertunnel_ng.netlib", Level.ERROR.toString());
    }
    LogbackUtils.setLogLevel(
        "org.silvertunnel_ng.netlib.layer.tor.directory.RouterParserCallable",
        Level.ERROR.toString());
    LogbackUtils.setLogLevel(
        "org.silvertunnel_ng.netlib.layer.tor.directory.Directory", Level.ERROR.toString());

    Optional<JavaTorClient> torClient = Optional.empty();

    if (log.isDebugEnabled()) {
      for (Map.Entry<String, String> entry : cliConfig.getConfigInfo().entrySet()) {
        log.debug("config/override: " + entry.getKey() + ": " + entry.getValue());
      }
    }
    try {
      // initialize bitcoinj context
      NetworkParameters params = cliConfig.getNetworkParameters();
      new Context(params);

      // no arg => show pools list
      if (appArgs.getPoolId() == null) {
        new RunListPools(whirlpoolClient).run();
        log.info("Tip: use --pool argument to select a pool");
      } else {
        // check pushTxService
        if (!pushTxService.testConnectivity()) {
          throw new NotifiableException("Unable to connect to pushTxService");
        }

        // init wallet
        cliWalletService.openWallet(appArgs.getSeedWords(), appArgs.getSeedPassphrase());

        // check upgrade wallet
        checkUpgradeWallet();

        if (isListen) {
          // --listen => listen for API commands
          new RunCliCommand(
                  appArgs,
                  samouraiApi,
                  whirlpoolClient,
                  whirlpoolClientConfig,
                  cliWalletService,
                  bech32Util,
                  walletAggregateService,
                  torClientService,
                  tx0Service)
              .run();
        } else {
          // execute requested command
          new RunCliCommand(
                  appArgs,
                  samouraiApi,
                  whirlpoolClient,
                  whirlpoolClientConfig,
                  cliWalletService,
                  bech32Util,
                  walletAggregateService,
                  torClientService,
                  tx0Service)
              .run();
        }
      }
    } catch (NotifiableException e) {
      log.error(e.getMessage());
    } catch (IllegalArgumentException e) {
      log.info("Invalid arguments: " + e.getMessage());
      log.info("Usage: whirlpool-client " + ApplicationArgs.USAGE);
    } catch (Exception e) {
      log.error("", e);
    }

    // disconnect
    if (torClient.isPresent()) {
      torClient.get().disconnect();
    }
    stompClient.disconnect();
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
}
