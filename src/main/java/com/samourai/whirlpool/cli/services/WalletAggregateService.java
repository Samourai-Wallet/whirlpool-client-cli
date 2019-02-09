package com.samourai.whirlpool.cli.services;

import com.samourai.api.client.SamouraiApi;
import com.samourai.api.client.beans.UnspentResponse;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WalletAggregateService {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int AGGREGATED_UTXOS_PER_TX = 500;

  private SamouraiApi samouraiApi;
  private PushTxService pushTxService;
  private NetworkParameters params;
  private CliConfig cliConfig;
  private CliWalletService cliWalletService;
  private Bech32UtilGeneric bech32Util;
  private TxAggregateService txAggregateService;

  public WalletAggregateService(
      SamouraiApi samouraiApi,
      PushTxService pushTxService,
      NetworkParameters params,
      CliConfig cliConfig,
      CliWalletService cliWalletService,
      Bech32UtilGeneric bech32Util,
      TxAggregateService txAggregateService) {
    this.samouraiApi = samouraiApi;
    this.pushTxService = pushTxService;
    this.params = params;
    this.cliConfig = cliConfig;
    this.cliWalletService = cliWalletService;
    this.bech32Util = bech32Util;
    this.txAggregateService = txAggregateService;
  }

  public boolean toWallet(Bip84ApiWallet sourceWallet, Bip84Wallet destinationWallet)
      throws Exception {
    return doAggregate(sourceWallet, null, destinationWallet);
  }

  public boolean toAddress(Bip84ApiWallet sourceWallet, String destinationAddress)
      throws Exception {
    return doAggregate(sourceWallet, destinationAddress, null);
  }

  private boolean doAggregate(
      Bip84ApiWallet sourceWallet, String destinationAddress, Bip84Wallet destinationWallet)
      throws Exception {
    List<UnspentResponse.UnspentOutput> utxos = sourceWallet.fetchUtxos();
    if (utxos.isEmpty()) {
      // maybe you need to declare zpub as bip84 with /multiaddr?bip84=
      log.info("AggregateWallet result: no utxo to aggregate");
      return false;
    }
    if (log.isDebugEnabled()) {
      log.debug("Found " + utxos.size() + " utxo to aggregate:");
      ClientUtils.logUtxos(utxos);
    }

    boolean success = false;
    int round = 0;
    int offset = 0;
    while (offset < utxos.size()) {
      List<UnspentResponse.UnspentOutput> subsetUtxos = new ArrayList<>();
      offset = AGGREGATED_UTXOS_PER_TX * round;
      for (int i = offset; i < (offset + AGGREGATED_UTXOS_PER_TX) && i < utxos.size(); i++) {
        subsetUtxos.add(utxos.get(i));
      }
      // allow aggregate 1 utxo when moving to specific address, otherwise 2 utxos min
      // (otherwise infinite loop on RunUpgrade)
      if (subsetUtxos.size() > 1 || (subsetUtxos.size() == 1 && destinationAddress != null)) {
        String toAddress = destinationAddress;
        if (toAddress == null) {
          toAddress = bech32Util.toBech32(destinationWallet.getNextAddress(), params);
        }

        log.info("Aggregating " + subsetUtxos.size() + " utxos (pass #" + round + ")");
        txAggregate(sourceWallet, subsetUtxos, toAddress);
        success = true;

        samouraiApi.refreshUtxos();
      }
      round++;
    }
    return success;
  }

  private void txAggregate(
      Bip84ApiWallet sourceWallet,
      List<UnspentResponse.UnspentOutput> postmixUtxos,
      String toAddress)
      throws Exception {
    List<TransactionOutPoint> spendFromOutPoints = new ArrayList<>();
    List<HD_Address> spendFromAddresses = new ArrayList<>();

    // spend
    for (UnspentResponse.UnspentOutput utxo : postmixUtxos) {
      spendFromOutPoints.add(utxo.computeOutpoint(params));
      spendFromAddresses.add(sourceWallet.getAddressAt(utxo));
    }

    int feeSatPerByte = samouraiApi.fetchFees();

    // tx
    Transaction txAggregate =
        txAggregateService.txAggregate(
            spendFromOutPoints, spendFromAddresses, toAddress, feeSatPerByte);

    log.info("txAggregate:");
    log.info(txAggregate.toString());

    // broadcast
    log.info(" • Broadcasting TxAggregate...");
    pushTxService.pushTx(txAggregate);
  }

  public boolean consolidateTestnet() throws Exception {
    if (!cliConfig.isTestnet()) {
      throw new NotifiableException(
          "consolidateTestnet cannot be run on mainnet for privacy reasons.");
    }

    Bip84ApiWallet depositWallet = cliWalletService.getSessionWallet().getWalletDeposit();
    Bip84ApiWallet premixWallet = cliWalletService.getSessionWallet().getWalletPremix();
    Bip84ApiWallet postmixWallet = cliWalletService.getSessionWallet().getWalletPostmix();

    log.info(" • Consolidating postmix -> deposit...");
    toWallet(postmixWallet, depositWallet);

    log.info(" • Consolidating premix -> deposit...");
    toWallet(premixWallet, depositWallet);

    log.info(" • Consolidating deposit...");
    boolean success = toWallet(depositWallet, depositWallet);
    return success;
  }
}
