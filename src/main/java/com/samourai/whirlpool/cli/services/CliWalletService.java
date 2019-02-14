package com.samourai.whirlpool.cli.services;

import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.client.indexHandler.FileIndexHandler;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.cli.wallet.CliWalletAccount;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import java.io.File;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CliWalletService extends WhirlpoolWalletService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int AUTOMIX_DELAY = 3 * 60 * 1000; // automix rescan delay for premix
  public static final String INDEX_BIP84_INITIALIZED = "bip84init";
  private static final String INDEX_DEPOSIT = "deposit";
  private static final String INDEX_DEPOSIT_CHANGE = "deposit_change";
  private static final String INDEX_PREMIX = "premix";
  private static final String INDEX_PREMIX_CHANGE = "premix_change";
  private static final String INDEX_POSTMIX = "postmix";
  private static final String INDEX_POSTMIX_CHANGE = "postmix_change";
  private static final String INDEX_FEE = "fee";

  private CliConfig cliConfig;
  private SamouraiApiService samouraiApiService;
  private FileIndexHandler fileIndexHandler;
  private HD_WalletFactoryJava hdWalletFactory;
  private WalletAggregateService walletAggregateService;

  // available when wallet is opened
  private CliWallet sessionWallet = null;
  private HD_Wallet bip84w = null;

  public CliWalletService(
      CliConfig cliConfig,
      SamouraiApiService samouraiApiService,
      PushTxService pushTxService,
      Tx0Service tx0Service,
      Bech32UtilGeneric bech32Util,
      WhirlpoolClient whirlpoolClient,
      WhirlpoolClientConfig whirlpoolClientConfig,
      HD_WalletFactoryJava hdWalletFactory,
      WalletAggregateService walletAggregateService) {
    super(
        cliConfig.getNetworkParameters(),
        samouraiApiService,
        pushTxService,
        tx0Service,
        bech32Util,
        whirlpoolClient,
        whirlpoolClientConfig,
        cliConfig.getMix().getClients(),
        cliConfig.getMix().getClientDelay(),
        cliConfig.getMix().isAutoTx0() ? cliConfig.getMix().getClientDelay() + 5 : 0,
        cliConfig.getMix().isAutoMix() ? AUTOMIX_DELAY : 0,
        cliConfig.getMix().getTx0Delay(),
        cliConfig.getMix().getPoolIdsByPriority());
    this.cliConfig = cliConfig;
    this.samouraiApiService = samouraiApiService;
    this.hdWalletFactory = hdWalletFactory;
    this.walletAggregateService = walletAggregateService;
  }

  public WhirlpoolWallet openWallet(String seedWords, String seedPassphrase) throws Exception {
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
            CliWalletAccount.DEPOSIT.getAccountIndex(),
            depositIndexHandler,
            depositChangeIndexHandler,
            samouraiApiService,
            initBip84);
    Bip84ApiWallet premixWallet =
        new Bip84ApiWallet(
            bip84w,
            CliWalletAccount.PREMIX.getAccountIndex(),
            premixIndexHandler,
            premixChangeIndexHandler,
            samouraiApiService,
            initBip84);
    Bip84ApiWallet postmixWallet =
        new Bip84ApiWallet(
            bip84w,
            CliWalletAccount.POSTMIX.getAccountIndex(),
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
    WhirlpoolWallet whirlpoolWallet =
        openWallet(feeIndexHandler, depositWallet, premixWallet, postmixWallet);
    this.sessionWallet = new CliWallet(whirlpoolWallet, cliConfig, walletAggregateService, this);
    return sessionWallet;
  }

  public void closeWallet() {
    if (this.sessionWallet != null) {
      this.sessionWallet.stop();
      this.sessionWallet = null;
      this.bip84w = null;
    }
  }

  public CliWallet getSessionWallet() throws NoSessionWalletException {
    if (sessionWallet == null) {
      throw new NoSessionWalletException();
    }
    return sessionWallet;
  }

  public HD_Wallet getBip84w() throws NoSessionWalletException {
    if (bip84w == null) {
      throw new NoSessionWalletException();
    }
    return bip84w;
  }

  public FileIndexHandler getFileIndexHandler() {
    return fileIndexHandler;
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
