package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.whirlpool.client.ApplicationArgs;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import com.samourai.whirlpool.client.utils.indexHandler.MemoryIndexHandler;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunUpgradeCli {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int CLI_V1 = 1;
  private static final int CLI_V2 = 2;
  private static final int CLI_V3 = 3;

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private Optional<RpcClientService> rpcClientService;
  private Bip84ApiWallet depositWallet;
  private Bip84ApiWallet premixWallet;
  private Bip84ApiWallet postmixWallet;
  private ApplicationArgs appArgs;

  public RunUpgradeCli(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      Optional<RpcClientService> rpcClientService,
      Bip84ApiWallet depositWallet,
      Bip84ApiWallet premixWallet,
      Bip84ApiWallet postmixWallet,
      ApplicationArgs appArgs) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.rpcClientService = rpcClientService;
    this.depositWallet = depositWallet;
    this.premixWallet = premixWallet;
    this.postmixWallet = postmixWallet;
    this.appArgs = appArgs;
  }

  public void run(int lastVersion) throws Exception {
    // run upgrades
    if (lastVersion < CLI_V1) {
      upgradeV1();
    }
    if (lastVersion < CLI_V2) {
      upgradeV2();
    }
    if (lastVersion < CLI_V3) {
      upgradeV3();
    }
  }

  private void upgradeV1() throws Exception {
    // consolidate premix to force new tx0
    log.info(" • Upgrade to new tx0 payload: consolidating premix wallet...");
    new RunAggregateWallet(params, samouraiApi, rpcClientService, premixWallet).run(depositWallet);
  }

  private void upgradeV2() throws Exception {
    // reinitialize bip84 wallets
    log.info(" • Upgrade to BIP84: reinitializing BIP84 wallets...");
    depositWallet.initBip84();
    depositWallet.getIndexHandler().set(0);

    premixWallet.initBip84();
    premixWallet.getIndexHandler().set(0);

    postmixWallet.initBip84();
    postmixWallet.getIndexHandler().set(0);

    // transfer BIP44 -> BIP84
    final int ACCOUNT_DEPOSIT_AND_PREMIX = 0;
    final int ACCOUNT_POSTMIX = Integer.MAX_VALUE - 1;

    HD_Wallet bip44w =
        HD_WalletFactoryJava.getInstance()
            .restoreWallet(appArgs.getSeedWords(), appArgs.getSeedPassphrase(), 1, params);
    Bip84ApiWallet depositAndPremixWallet44 =
        new Bip84ApiWallet(
            bip44w, ACCOUNT_DEPOSIT_AND_PREMIX, new MemoryIndexHandler(), samouraiApi, false);
    Bip84ApiWallet postmixWallet44 =
        new Bip84ApiWallet(bip44w, ACCOUNT_POSTMIX, new MemoryIndexHandler(), samouraiApi, false);

    log.info(" • Upgrade to BIP84: transferring BIP44 to BIP84: depositAndPremix...");
    new RunAggregateWallet(params, samouraiApi, rpcClientService, depositAndPremixWallet44)
        .run(depositWallet);
    log.info(" • Upgrade to BIP84: transferring BIP44 to BIP84: postmix...");
    new RunAggregateWallet(params, samouraiApi, rpcClientService, postmixWallet44)
        .run(depositWallet);
  }

  private void upgradeV3() throws Exception {
    // initialize premix wallet
    log.info(" • Upgrade premix wallet: initializing premix wallet...");
    premixWallet.initBip84();
    premixWallet.getIndexHandler().set(0);

    log.info(" • Upgrade premix wallet: aggregate deposit...");
    new RunAggregateWallet(params, samouraiApi, rpcClientService, depositWallet).run(depositWallet);
  }
}
