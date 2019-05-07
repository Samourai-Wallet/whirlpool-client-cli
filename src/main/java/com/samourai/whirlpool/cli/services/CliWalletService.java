package com.samourai.whirlpool.cli.services;

import com.google.common.primitives.Bytes;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.tor.client.JavaTorConnexion;
import com.samourai.wallet.client.indexHandler.IIndexHandler;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.whirlpool.cli.beans.CliState;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.exception.NoSessionWalletException;
import com.samourai.whirlpool.cli.run.RunUpgradeCli;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.WhirlpoolWalletService;
import com.samourai.whirlpool.client.wallet.persist.FileWhirlpoolWalletPersistHandler;
import com.samourai.whirlpool.client.wallet.persist.WhirlpoolWalletPersistHandler;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import javax.crypto.AEADBadTagException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CliWalletService extends WhirlpoolWalletService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int CLI_VERSION = 3;

  private static final String INDEX_CLI_VERSION = "cliVersion";

  private CliConfig cliConfig;
  private CliConfigService cliConfigService;
  private HD_WalletFactoryJava hdWalletFactory;
  private WalletAggregateService walletAggregateService;
  private JavaStompClient stompClient;
  private CliTorClientService cliTorClientService;

  // available when wallet is opened
  private CliWallet sessionWallet = null;

  public CliWalletService(
      CliConfig cliConfig,
      CliConfigService cliConfigService,
      HD_WalletFactoryJava hdWalletFactory,
      WalletAggregateService walletAggregateService,
      JavaStompClient stompClient,
      CliTorClientService cliTorClientService) {
    super(
        cliConfig
            .computeWhirlpoolWalletConfig()); // TODO won't honor --tor as config is not overriden
    // yet
    this.cliConfig = cliConfig;
    this.cliConfigService = cliConfigService;
    this.hdWalletFactory = hdWalletFactory;
    this.walletAggregateService = walletAggregateService;
    this.stompClient = stompClient;
    this.cliTorClientService = cliTorClientService;
  }

  public CliWallet openWallet(String seedPassphrase) throws Exception {
    // require CliStatus.READY
    if (!CliStatus.READY.equals(cliConfigService.getCliStatus())) {
      throw new NotifiableException(
          "Cannot start wallet: cliStatus=" + cliConfigService.getCliStatus());
    }

    NetworkParameters params = cliConfig.getServer().getParams();

    String seedWords;
    try {
      seedWords = decryptSeedWords(cliConfig.getSeed(), seedPassphrase);
    } catch (Exception e) {
      log.error("decryptSeedWords failed, invalid passphrase?");
      if (log.isDebugEnabled() && !(e instanceof AEADBadTagException)) {
        log.debug("", e);
      }
      throw new NotifiableException("Seed decrypt failed, invalid passphrase?");
    }

    // identifier
    String walletIdentifier;
    HD_Wallet bip84w;
    try {
      // init wallet from seed
      byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
      bip84w = hdWalletFactory.getBIP84(seed, seedPassphrase, params);
      walletIdentifier = computeWalletIdentifier(seed, seedPassphrase, params);
    } catch (MnemonicException e) {
      throw new NotifiableException("Mnemonic failed, invalid passphrase?");
    }

    // open wallet
    File indexFile = computeIndexFile(walletIdentifier);
    File utxosFile = computeUtxosFile(walletIdentifier);
    WhirlpoolWalletPersistHandler walletPersistHandler =
        new FileWhirlpoolWalletPersistHandler(indexFile, utxosFile);
    WhirlpoolWallet whirlpoolWallet = openWallet(bip84w, walletPersistHandler);
    this.sessionWallet =
        new CliWallet(
            whirlpoolWallet,
            cliConfig,
            walletAggregateService,
            stompClient,
            cliTorClientService,
            this);

    // check upgrade wallet
    checkUpgradeWallet(whirlpoolWallet);

    return sessionWallet;
  }

  protected String decryptSeedWords(String seedWordsEncrypted, String seedPassphrase)
      throws Exception {
    return AESUtil.decrypt(seedWordsEncrypted, new CharSequenceX(seedPassphrase));
  }

  public void closeWallet() {
    if (this.sessionWallet != null) {
      this.sessionWallet.stop();
      this.sessionWallet = null;
    }
  }

  public CliWallet getSessionWallet() throws NoSessionWalletException {
    if (sessionWallet == null) {
      throw new NoSessionWalletException();
    }
    return sessionWallet;
  }

  public boolean hasSessionWallet() {
    return sessionWallet != null;
  }

  private String computeWalletIdentifier(
      byte[] seed, String seedPassphrase, NetworkParameters params) {
    return ClientUtils.sha256Hash(
        Bytes.concat(seed, seedPassphrase.getBytes(), params.getId().getBytes()));
  }

  private File computeIndexFile(String walletIdentifier) throws NotifiableException {
    String path = "whirlpool-cli-state-" + walletIdentifier + ".json";
    if (log.isDebugEnabled()) {
      log.debug("indexFile: " + path);
    }
    return computeFile(path);
  }

  private File computeUtxosFile(String walletIdentifier) throws NotifiableException {
    String path = "whirlpool-cli-utxos-" + walletIdentifier + ".json";
    if (log.isDebugEnabled()) {
      log.debug("utxosFile: " + path);
    }
    return computeFile(path);
  }

  private File computeFile(String path) throws NotifiableException {
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

  private void checkUpgradeWallet(WhirlpoolWallet whirlpoolWallet) throws Exception {
    IIndexHandler cliVersionHandler =
        whirlpoolWallet.getWalletPersistHandler().getIndexHandler(INDEX_CLI_VERSION, CLI_VERSION);
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
    new RunUpgradeCli(this).run(lastVersion);

    // set new version
    cliVersionHandler.set(CLI_VERSION);
  }

  public CliState getCliState() {
    CliStatus cliStatus = cliConfigService.getCliStatus();
    String cliMessage = cliConfigService.getCliMessage();
    boolean loggedIn = hasSessionWallet();

    Optional<JavaTorConnexion> torConnexion = cliTorClientService.getTorConnexion(false);
    Integer torProgress = torConnexion.isPresent() ? torConnexion.get().getProgress() : null;
    return new CliState(cliStatus, cliMessage, loggedIn, torProgress);
  }
}
