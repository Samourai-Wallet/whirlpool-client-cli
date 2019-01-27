package com.samourai.whirlpool.cli.services;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.client.indexHandler.FileIndexHandler;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoWalletException;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import java.io.File;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CliWalletService extends WhirlpoolWalletService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int ACCOUNT_DEPOSIT = 0;
  private static final int ACCOUNT_PREMIX = Integer.MAX_VALUE - 2;
  private static final int ACCOUNT_POSTMIX = Integer.MAX_VALUE - 1;

  public static final String INDEX_BIP84_INITIALIZED = "bip84init";
  private static final String INDEX_DEPOSIT = "deposit";
  private static final String INDEX_DEPOSIT_CHANGE = "deposit_change";
  private static final String INDEX_PREMIX = "premix";
  private static final String INDEX_PREMIX_CHANGE = "premix_change";
  private static final String INDEX_POSTMIX = "postmix";
  private static final String INDEX_POSTMIX_CHANGE = "postmix_change";
  private static final String INDEX_FEE = "fee";

  private static final String FEE_XPUB =
      "vpub5YS8pQgZKVbrSn9wtrmydDWmWMjHrxL2mBCZ81BDp7Z2QyCgTLZCrnBprufuoUJaQu1ZeiRvUkvdQTNqV6hS96WbbVZgweFxYR1RXYkBcKt";
  private static final long FEE_VALUE = 10000; // TODO

  private CliConfig cliConfig;
  private SamouraiApiService samouraiApiService;
  private PushTxService pushTxService;
  private WhirlpoolClient whirlpoolClient;
  private FileIndexHandler fileIndexHandler;
  private HD_WalletFactoryJava hdWalletFactory;

  // available when wallet is opened
  private CliWallet cliWallet = null;
  private HD_Wallet bip84w = null;
  private Pools pools = null;

  public CliWalletService(
      CliConfig cliConfig,
      SamouraiApiService samouraiApiService,
      PushTxService pushTxService,
      WhirlpoolClient whirlpoolClient,
      HD_WalletFactoryJava hdWalletFactory) {
    super(
        cliConfig.getNetworkParameters(),
        samouraiApiService,
        pushTxService,
        new Tx0Service(cliConfig.getNetworkParameters(), FEE_XPUB, FEE_VALUE),
        whirlpoolClient);
    this.cliConfig = cliConfig;
    this.samouraiApiService = samouraiApiService;
    this.pushTxService = pushTxService;
    this.whirlpoolClient = whirlpoolClient;
    this.hdWalletFactory = hdWalletFactory;
  }

  public CliWallet openWallet(String seedWords, String seedPassphrase) throws Exception {
    NetworkParameters params = cliConfig.getNetworkParameters();

    // init fileIndexHandler
    String walletIdentifier =
        CliUtils.sha256Hash(seedPassphrase + seedWords + cliConfig.getNetworkParameters().getId());
    this.fileIndexHandler = new FileIndexHandler(computeIndexFile(walletIdentifier));

    // init wallet from seed
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    this.bip84w = hdWalletFactory.getBIP84(seed, seedPassphrase, params);

    // init bip84 at first run
    boolean initBip84 = (fileIndexHandler.get(INDEX_BIP84_INITIALIZED) != 1);

    // deposit, premix & postmix wallets
    IIndexHandler depositIndexHandler = fileIndexHandler.getIndexHandler(INDEX_DEPOSIT);
    IIndexHandler depositChangeIndexHandler =
        fileIndexHandler.getIndexHandler(INDEX_DEPOSIT_CHANGE);
    IIndexHandler premixIndexHandler = fileIndexHandler.getIndexHandler(INDEX_PREMIX);
    IIndexHandler premixChangeIndexHandler = fileIndexHandler.getIndexHandler(INDEX_PREMIX_CHANGE);
    IIndexHandler postmixIndexHandler = fileIndexHandler.getIndexHandler(INDEX_POSTMIX);
    IIndexHandler postmixChangeIndexHandler =
        fileIndexHandler.getIndexHandler(INDEX_POSTMIX_CHANGE);
    Bip84ApiWallet depositWallet =
        new Bip84ApiWallet(
            bip84w,
            ACCOUNT_DEPOSIT,
            depositIndexHandler,
            depositChangeIndexHandler,
            samouraiApiService,
            initBip84);
    Bip84ApiWallet premixWallet =
        new Bip84ApiWallet(
            bip84w,
            ACCOUNT_PREMIX,
            premixIndexHandler,
            premixChangeIndexHandler,
            samouraiApiService,
            initBip84);
    Bip84ApiWallet postmixWallet =
        new Bip84ApiWallet(
            bip84w,
            ACCOUNT_POSTMIX,
            postmixIndexHandler,
            postmixChangeIndexHandler,
            samouraiApiService,
            initBip84);

    // save initialized state
    if (initBip84) {
      fileIndexHandler.set(INDEX_BIP84_INITIALIZED, 1);
    }

    // log zpubs
    if (log.isDebugEnabled()) {
      String depositZpub = depositWallet.getZpub();
      String premixZpub = premixWallet.getZpub();
      String postmixZpub = postmixWallet.getZpub();
      log.debug(
          "Using wallet deposit: accountIndex="
              + depositWallet.getAccountIndex()
              + ", zpub="
              + depositZpub);
      log.debug(
          "Using wallet premix: accountIndex="
              + premixWallet.getAccountIndex()
              + ", zpub="
              + premixZpub);
      log.debug(
          "Using wallet postmix: accountIndex="
              + postmixWallet.getAccountIndex()
              + ", zpub="
              + postmixZpub);
    }

    // services
    IIndexHandler feeIndexHandler = fileIndexHandler.getIndexHandler(INDEX_FEE);
    this.cliWallet =
        new CliWallet(openWallet(feeIndexHandler, depositWallet, premixWallet, postmixWallet));
    return cliWallet;
  }

  public void closeWallet() {
    this.cliWallet = null;
    this.bip84w = null;
    this.pools = null;
  }

  public Tx0 tx0(String poolId, int nbOutputsPreferred, int nbOutputsMin) throws Exception {
    Pool pool = findPool(poolId);
    return getCliWallet().tx0(pool, nbOutputsPreferred, nbOutputsMin);
  }

  public CliWallet getCliWallet() throws NoWalletException {
    if (cliWallet == null) {
      throw new NoWalletException();
    }
    return cliWallet;
  }

  public HD_Wallet getBip84w() throws NoWalletException {
    if (bip84w == null) {
      throw new NoWalletException();
    }
    return bip84w;
  }

  public FileIndexHandler getFileIndexHandler() {
    return fileIndexHandler;
  }

  private Pool findPool(String poolId) throws Exception {
    Pool pool = getPools().findPoolById(poolId);
    if (pool == null) {
      throw new NotifiableException("Pool not found: " + poolId);
    }
    return pool;
  }

  private Pools getPools() throws Exception {
    if (this.pools == null) {
      this.pools = whirlpoolClient.fetchPools();
    }
    return this.pools;
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
}
