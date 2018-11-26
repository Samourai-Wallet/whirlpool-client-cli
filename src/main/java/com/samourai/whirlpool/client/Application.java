package com.samourai.whirlpool.client;

import com.samourai.api.SamouraiApi;
import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.rpc.client.JSONRpcClientServiceImpl;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.tor.client.JavaTorClient;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.exception.BroadcastException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.run.RunAggregateAndConsolidateWallet;
import com.samourai.whirlpool.client.run.RunAggregateWallet;
import com.samourai.whirlpool.client.run.RunListPools;
import com.samourai.whirlpool.client.run.RunLoopWallet;
import com.samourai.whirlpool.client.run.RunMixUtxo;
import com.samourai.whirlpool.client.run.RunMixWallet;
import com.samourai.whirlpool.client.run.RunTx0;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import com.samourai.whirlpool.client.utils.CliUtils;
import com.samourai.whirlpool.client.utils.LogbackUtils;
import com.samourai.whirlpool.client.utils.indexHandler.FileIndexHandler;
import com.samourai.whirlpool.client.utils.indexHandler.IIndexHandler;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Optional;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/** Command-line client. */
@EnableAutoConfiguration
public class Application implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int ACCOUNT_DEPOSIT_AND_PREMIX = 0;
  private static final int ACCOUNT_POSTMIX = Integer.MAX_VALUE - 1;
  private static final int SLEEP_LOOPWALLET_ON_ERROR = 30000;
  private static final String INDEX_DEPOSIT_AND_PREMIX = "depositAndPremix";
  private static final String INDEX_POSTMIX = "postmix";
  private static final String INDEX_FEE = "fee";

  private ApplicationArgs appArgs;
  private HD_WalletFactoryJava hdWalletFactory = HD_WalletFactoryJava.getInstance();

  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(ApplicationArguments args) {
    this.appArgs = new ApplicationArgs(args);

    // enable debug logs with --debug
    if (appArgs.isDebug()) {
      LogbackUtils.setLogLevel("com.samourai", Level.DEBUG.toString());
    }

    Optional<JavaTorClient> torClient = Optional.empty();
    WhirlpoolClientConfig config = null;

    log.info("------------ whirlpool-client ------------");
    log.info(
        "Running whirlpool-client {} on java {}",
        Arrays.toString(args.getSourceArgs()),
        System.getProperty("java.version"));
    try {
      NetworkParameters params = appArgs.getNetworkParameters();
      new Context(params); // initialize bitcoinj context

      // TOR
      if (appArgs.isTor()) {
        torClient = Optional.of(new JavaTorClient());
        torClient.get().connect();
      }

      // instanciate client
      String server = appArgs.getServer();
      config = computeWhirlpoolClientConfig(server, params, torClient);
      WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(config);

      // fetch pools
      try {
        log.info(" • Fetching pools...");
        Pools pools = whirlpoolClient.fetchPools();

        String poolId = appArgs.getPoolId();
        if (poolId != null) {
          // if --pool is provided, find pool
          Pool pool = pools.findPoolById(poolId);
          if (pool != null) {
            // pool found
            String seedWords = appArgs.getSeedWords();
            String seedPassphrase = appArgs.getSeedPassphrase();
            HD_Wallet bip84w = hdWalletFactory.restoreWallet(seedWords, seedPassphrase, 1, params);

            if (appArgs.isUtxo()) {
              // go whirlpool with UTXO
              String utxoHash = appArgs.getUtxoHash();
              long utxoIdx = appArgs.getUtxoIdx();
              String utxoKey = appArgs.getUtxoKey();
              long utxoBalance = appArgs.getUtxoBalance();
              int paynymIndex = appArgs.getPaynymIndex();
              final int mixs = appArgs.getMixs();

              new RunMixUtxo()
                  .run(
                      whirlpoolClient,
                      pool,
                      params,
                      utxoHash,
                      utxoIdx,
                      utxoKey,
                      utxoBalance,
                      bip84w,
                      paynymIndex,
                      mixs);
            } else {
              Optional<RpcClientService> rpcClientService = computeRpcClientService(appArgs);
              SamouraiApi samouraiApi = new SamouraiApi(config.getHttpClient());

              // indexes
              String walletIdentifier =
                  CliUtils.sha256Hash(seedPassphrase + seedWords + params.getId());
              FileIndexHandler fileIndexHandler =
                  new FileIndexHandler(computeIndexFile(walletIdentifier));
              IIndexHandler depositAndPremixIndexHandler =
                  fileIndexHandler.getIndexHandler(INDEX_DEPOSIT_AND_PREMIX);
              IIndexHandler postmixIndexHandler = fileIndexHandler.getIndexHandler(INDEX_POSTMIX);
              IIndexHandler feeIndexHandler = fileIndexHandler.getIndexHandler(INDEX_FEE);
              // --postmix-index
              Integer postmixIndex = appArgs.getPostmixIndex();
              if (postmixIndex != null) {
                postmixIndexHandler.set(postmixIndex);
              }

              // init wallets
              Bip84ApiWallet depositAndPremixWallet =
                  new Bip84ApiWallet(
                      bip84w,
                      ACCOUNT_DEPOSIT_AND_PREMIX,
                      depositAndPremixIndexHandler,
                      samouraiApi);
              Bip84ApiWallet postmixWallet =
                  new Bip84ApiWallet(bip84w, ACCOUNT_POSTMIX, postmixIndexHandler, samouraiApi);
              RunTx0 runTx0 =
                  new RunTx0(params, samouraiApi, rpcClientService, depositAndPremixWallet);
              RunAggregateAndConsolidateWallet runAggregateAndConsolidateWallet =
                  new RunAggregateAndConsolidateWallet(
                      params, samouraiApi, rpcClientService, depositAndPremixWallet, postmixWallet);

              // init wallets
              initWallets(depositAndPremixWallet, postmixWallet, fileIndexHandler, samouraiApi);

              Optional<Integer> tx0Arg = appArgs.getTx0();
              if (tx0Arg.isPresent()) {
                // go tx0
                runTx0.runTx0(pool, tx0Arg.get(), pools.getFeePaymentCode(), feeIndexHandler);
              } else if (appArgs.isAggregatePostmix()) {
                if (!FormatsUtilGeneric.getInstance().isTestNet(params)) {
                  throw new NotifiableException(
                      "AggregatePostmix cannot be run on mainnet for privacy reasons.");
                }

                // go aggregate and consolidate
                runAggregateAndConsolidateWallet.run();

                // should we move to a specific address?
                String toAddress = appArgs.getAggregatePostmix();
                if (toAddress != null) {
                  log.info(" • Moving funds to: " + toAddress);
                  new RunAggregateWallet(
                          params, samouraiApi, rpcClientService, depositAndPremixWallet)
                      .run(toAddress);
                }
              } else {
                // go loop wallet
                int iterationDelay = appArgs.getIterationDelay();
                iterationDelay =
                    Math.max(
                        SamouraiApi.SLEEP_REFRESH_UTXOS / 1000,
                        iterationDelay); // wait for API to refresh
                int clientDelay = appArgs.getClientDelay();
                int clients = appArgs.getClients();
                if (torClient.isPresent()) {
                  torClient.get().setNbPrivateConnexions(clients * 2);
                }
                RunMixWallet runMixWallet =
                    new RunMixWallet(
                        config,
                        torClient,
                        depositAndPremixWallet,
                        postmixWallet,
                        clientDelay * 1000,
                        clients);
                Optional<RunAggregateAndConsolidateWallet>
                    optionalRunAggregateAndConsolidateWallet =
                        appArgs.isAutoAggregatePostmix()
                            ? Optional.of(runAggregateAndConsolidateWallet)
                            : Optional.empty();
                RunLoopWallet runLoopWallet =
                    new RunLoopWallet(
                        config,
                        runTx0,
                        runMixWallet,
                        depositAndPremixWallet,
                        optionalRunAggregateAndConsolidateWallet);
                int i = 1;
                int errors = 0;
                while (true) {
                  try {
                    boolean success =
                        runLoopWallet.run(
                            pool, clients, pools.getFeePaymentCode(), feeIndexHandler);
                    if (!success) {
                      throw new NotifiableException("Iteration failed");
                    }

                    log.info(
                        " ✔ Cycle #"
                            + i
                            + " SUCCESS. Next cycle in "
                            + iterationDelay
                            + "s...  (total success: "
                            + (i - errors)
                            + ", errors: "
                            + errors
                            + ", postmixIndex: "
                            + postmixWallet.getIndexHandler().get()
                            + ")");
                    if (iterationDelay > 0) {
                      Thread.sleep(iterationDelay * 1000);
                    }
                  } catch (BroadcastException e) {
                    CliUtils.broadcastTxInstruction(e);
                  } catch (Exception e) {
                    log.error(e.getMessage());
                    errors++;
                    if (e instanceof NotifiableException) {
                      // don't log exception
                      log.error(
                          " ✖ Cycle #"
                              + i
                              + " FAILED, retrying in "
                              + (SLEEP_LOOPWALLET_ON_ERROR / 1000)
                              + "s (total errors: "
                              + errors
                              + ", postmixIndex: "
                              + postmixWallet.getIndexHandler().get()
                              + ")");
                    } else {
                      // log exception
                      log.error(
                          " ✖ Cycle #"
                              + i
                              + " FAILED, retrying in "
                              + (SLEEP_LOOPWALLET_ON_ERROR / 1000)
                              + "s (total errors: "
                              + errors
                              + ", postmixIndex: "
                              + postmixWallet.getIndexHandler().get()
                              + ")",
                          e);
                    }
                    Thread.sleep(SLEEP_LOOPWALLET_ON_ERROR);
                  }
                  i++;
                }
              }
            }
          } else {
            log.error("Pool not found: " + poolId);
          }
        } else {
          // show pools list if --pool is not provided/found
          new RunListPools().run(pools);
          log.info("Tip: use --pool argument to select a pool");
        }
      } catch (BroadcastException e) {
        try {
          CliUtils.broadcastTxInstruction(e);
        } catch (NotifiableException ee) {
          log.error(e.getMessage());
        }
      } catch (NotifiableException e) {
        log.error(e.getMessage());
      } catch (Exception e) {
        log.error("", e);
      }
    } catch (IllegalArgumentException e) {
      log.info("Invalid arguments: " + e.getMessage());
      log.info("Usage: whirlpool-client " + ApplicationArgs.USAGE);
    }

    // disconnect
    if (torClient.isPresent()) {
      torClient.get().disconnect();
    }
    if (config != null) {
      config.getStompClient().disconnect();
    }
  }

  private WhirlpoolClientConfig computeWhirlpoolClientConfig(
      String server, NetworkParameters params, Optional<JavaTorClient> torClient) {
    IHttpClient httpClient = new JavaHttpClient(torClient);
    IStompClient stompClient = new JavaStompClient(torClient);
    WhirlpoolClientConfig config =
        new WhirlpoolClientConfig(httpClient, stompClient, server, params);
    if (appArgs.isTestMode()) {
      config.setTestMode(true);
      if (log.isDebugEnabled()) {
        log.debug("--test-mode: tx0 verifications will be skiped (if server allows it)");
      }
    }
    boolean ssl = appArgs.isSsl();
    config.setSsl(ssl);
    return config;
  }

  private Optional<RpcClientService> computeRpcClientService(ApplicationArgs appArgs)
      throws Exception {
    String rpcClientUrl = appArgs.getRpcClientUrl();
    if (rpcClientUrl == null) {
      return Optional.empty();
    }
    NetworkParameters params = appArgs.getNetworkParameters();
    boolean isTestnet = FormatsUtilGeneric.getInstance().isTestNet(params);
    RpcClientService rpcClientService = new JSONRpcClientServiceImpl(rpcClientUrl, isTestnet);
    if (!rpcClientService.testConnectivity()) {
      throw new NotifiableException("Unable to connect to rpc-client-url");
    }
    return Optional.of(rpcClientService);
  }

  private File computeIndexFile(String walletIdentifier) throws NotifiableException {
    String path = "whirlpool-cli-state-" + walletIdentifier + ".json";
    if (log.isDebugEnabled()) {
      log.debug("indexFile: " + path);
    }
    File f = new File(path);
    if (!f.exists()) {
      if (log.isDebugEnabled()) {
        log.debug("Creating file " + path);
      }
      try {
        f.createNewFile();
      } catch (Exception e) {
        throw new NotifiableException("Unable to write file " + path);
      }
    }
    return f;
  }

  private void initWallets(
      Bip84ApiWallet depositAndPremixWallet,
      Bip84ApiWallet postmixWallet,
      FileIndexHandler fileIndexHandler,
      SamouraiApi samouraiApi)
      throws Exception {
    String depositAndPremixZpub = depositAndPremixWallet.getZpub();
    String postmixZpub = postmixWallet.getZpub();

    // log zpubs
    if (log.isDebugEnabled()) {
      log.debug(
          "Using wallet depositAndPremix: accountIndex="
              + depositAndPremixWallet.getAccountIndex()
              + ", zpub="
              + depositAndPremixZpub);
      log.debug(
          "Using wallet postmix: accountIndex="
              + postmixWallet.getAccountIndex()
              + ", zpub="
              + postmixZpub);
    }

    // init bip84 at first run
    if (fileIndexHandler.get(FileIndexHandler.BIP84_INITIALIZED) != 1) {
      log.info(" • Initializing bip84 wallet: depositAndPremix");
      samouraiApi.initBip84(depositAndPremixZpub);

      log.info(" • Initializing bip84 wallet: postmix");
      samouraiApi.initBip84(postmixZpub);

      fileIndexHandler.set(FileIndexHandler.BIP84_INITIALIZED, 1);
    }
  }
}
