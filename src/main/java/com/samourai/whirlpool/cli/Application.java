package com.samourai.whirlpool.cli;

import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.run.RunCliCommand;
import com.samourai.whirlpool.cli.run.RunCliInit;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.LogbackUtils;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
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
import org.springframework.core.env.Environment;

/** Command-line client. */
@SpringBootApplication
@ServletComponentScan(value = "com.samourai.whirlpool.cli.config.filters")
public class Application implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Integer listenPort;
  private static boolean debug;
  private static boolean debugClient;
  private static ConfigurableApplicationContext applicationContext;
  private static int exitCode = 0;

  @Autowired Environment env;
  @Autowired private ApplicationArgs appArgs;
  @Autowired private CliConfig cliConfig;
  @Autowired private CliConfigService cliConfigService;
  @Autowired private CliWalletService cliWalletService;
  private static CliWalletService cliWalletServiceStatic;
  @Autowired private PushTxService pushTxService;
  @Autowired private Bech32UtilGeneric bech32Util;
  @Autowired private WalletAggregateService walletAggregateService;
  @Autowired private CliTorClientService cliTorClientService;
  private static CliTorClientService cliTorClientServiceStatic;
  @Autowired private IHttpClient httpClient;

  public static void main(String... args) {
    // override configuration with local file
    System.setProperty(
        "spring.config.location",
        "classpath:application.properties,./" + CliConfigService.CLI_CONFIG_FILENAME);

    ApplicationArgs.setMainArgs(args);

    // start REST api if --listen
    listenPort = ApplicationArgs.getMainListen();
    WebApplicationType wat =
        listenPort != null ? WebApplicationType.SERVLET : WebApplicationType.NONE;
    if (listenPort != null) {
      System.setProperty("server.port", Integer.toString(listenPort));
    }

    // enable debug logs with --debug
    debug = ApplicationArgs.isMainDebug();
    debugClient = ApplicationArgs.isMainDebugClient();
    setDebug(debug, debugClient);

    // run
    applicationContext =
        new SpringApplicationBuilder(Application.class).logStartupInfo(debug).web(wat).run(args);

    shutdown();

    // exit with exitCode
    applicationContext.close();
    System.exit(exitCode);
  }

  @Override
  public void run(ApplicationArguments args) {
    // set static references from autowire
    cliWalletServiceStatic = cliWalletService;
    cliTorClientServiceStatic = cliTorClientService;

    log.info("------------ whirlpool-client-cli starting ------------");
    setup(args);

    if (env.acceptsProfiles(CliUtils.SPRING_PROFILE_TESTING)) {
      log.info("Running unit test...");
      return;
    }
    try {
      runCli();
    } catch (NotifiableException e) {
      exitCode = 1;
      CliUtils.notifyError(e.getMessage());
    } catch (IllegalArgumentException e) {
      exitCode = 1;
      log.info("Invalid arguments: " + e.getMessage());
    } catch (Exception e) {
      exitCode = 1;
      log.error("", e);
    }

    log.info("------------ whirlpool-client-cli ending ------------");
  }

  private void setup(ApplicationArguments args) {
    // log configuration
    setDebug(debug, debugClient); // run twice to fix incorrect log level

    // properties were just set on CliConfig => override CliConfig with cli args
    if (log.isDebugEnabled()) {
      log.debug("Overriding cliConfigFile with CLI args");
    }
    appArgs.override(cliConfig);

    log.info(
        "Running whirlpool-client {} on java {}",
        Arrays.toString(args.getSourceArgs()),
        System.getProperty("java.version"));
    if (log.isDebugEnabled()) {
      log.debug("[config/listen] " + (listenPort != null ? listenPort : "false"));
      log.debug("[config/debug] debug=" + debug + ", debugClient=" + debugClient);
      for (Map.Entry<String, String> entry : cliConfig.getConfigInfo().entrySet()) {
        log.debug("[cliConfig/" + entry.getKey() + "] " + entry.getValue());
      }
    }

    // setup proxy
    Optional<CliProxy> cliProxyOptional = cliConfig.getCliProxy();
    if (cliProxyOptional.isPresent()) {
      CliProxy cliProxy = cliProxyOptional.get();
      log.info("PROXY is ENABLED. Using: " + cliProxy);
      CliUtils.useProxy(cliProxy);
    } else {
      log.info("PROXY is DISABLED.");
    }
  }

  private void runCli() throws Exception {
    // connect TOR
    cliTorClientService.connect();

    // initialize bitcoinj context
    NetworkParameters params = cliConfig.getServer().getParams();
    new Context(params);

    // check init
    if (appArgs.isInit() || (cliConfigService.isCliStatusNotInitialized() && listenPort == null)) {
      new RunCliInit(appArgs, cliConfigService, cliWalletService).run();
      return;
    }

    // check pushTxService
    if (!pushTxService.testConnectivity()) {
      throw new NotifiableException("Unable to connect to pushTxService");
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
      return;
    }

    // check upgrade
    boolean shouldRestart = cliConfigService.checkUpgrade();
    if (shouldRestart) {
      log.warn("⣿ UPGRADE SUCCESS");
      log.warn("⣿ Please restart CLI.");
      return;
    }

    if (!appArgs.isAuthenticate()
        && listenPort != null
        && !RunCliCommand.hasCommandToRun(appArgs, cliConfig)) {
      // no passphrase but listening => keep listening
      log.info(CliUtils.LOG_SEPARATOR);
      log.info("⣿ AUTHENTICATION REQUIRED");
      log.info("⣿ Whirlpool wallet is CLOSED.");
      log.info("⣿ Please start GUI to authenticate and start mixing.");
      log.info("⣿ Or authenticate with --authenticate");
      log.info(CliUtils.LOG_SEPARATOR);
      keepRunning();
      return;
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
      new RunCliCommand(appArgs, cliWalletService, walletAggregateService, cliConfig).run();
    } else {
      // start wallet
      cliWallet.start();

      // keep cli running
      cliWallet.interactive();
    }
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

  private String authenticate() {
    log.info(CliUtils.LOG_SEPARATOR);
    log.info("⣿ AUTHENTICATION REQUIRED");
    log.info("⣿ Whirlpool wallet is CLOSED.");
    log.info("⣿ • Please type your seed passphrase to authenticate and start mixing.");
    return CliUtils.readUserInputRequired("Seed passphrase?", true);
  }

  private static void shutdown() {
    if (log.isDebugEnabled()) {
      log.debug("shutdown");
    }
    try {
      CliWallet cliWallet =
          cliWalletServiceStatic != null && cliWalletServiceStatic.hasSessionWallet()
              ? cliWalletServiceStatic.getSessionWallet()
              : null;
      // stop cliWallet
      if (cliWallet != null && cliWallet.isStarted()) {
        cliWallet.stop();
      }
    } catch (NoSessionWalletException e) {
    }

    // disconnect TOR
    if (cliTorClientServiceStatic != null) {
      cliTorClientServiceStatic.shutdown();
    }
  }

  private static void setDebug(boolean isDebug, boolean isDebugClient) {
    if (isDebug) {
      LogbackUtils.setLogLevel("com.samourai", Level.DEBUG.toString());
      // Utils.setLoggerDebug("org.springframework.security");
    } else {
      LogbackUtils.setLogLevel("org.silvertunnel_ng.netlib", Level.WARN.toString());
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
      LogbackUtils.setLogLevel(
          "com.samourai.whirlpool.client.wallet.orchestrator", Level.DEBUG.toString());
    }

    // skip noisy logs
    LogbackUtils.setLogLevel("org.bitcoinj", Level.ERROR.toString());
    LogbackUtils.setLogLevel("com.msopentech.thali.toronionproxy", Level.WARN.toString());
    LogbackUtils.setLogLevel("com.msopentech.thali.java.toronionproxy", Level.WARN.toString());
    LogbackUtils.setLogLevel("org.springframework.web", Level.INFO.toString());
    LogbackUtils.setLogLevel("org.apache.http.impl.conn", Level.INFO.toString());

    // LogbackUtils.setLogLevel("com", Level.DEBUG.toString());
    // LogbackUtils.setLogLevel("org", Level.DEBUG.toString());
  }
}
