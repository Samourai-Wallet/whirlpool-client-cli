package com.samourai.whirlpool.cli.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.api.client.beans.UnspentResponse;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.tx0.TxAggregateService;
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

public class RunAggregateWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int AGGREGATED_UTXOS_PER_TX = 500;
  protected static final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private PushTxService pushTxService;
  private Bip84ApiWallet sourceWallet;

  public RunAggregateWallet(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      PushTxService pushTxService,
      Bip84ApiWallet sourceWallet) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.pushTxService = pushTxService;
    this.sourceWallet = sourceWallet;
  }

  public boolean run(Bip84Wallet destinationWallet) throws Exception {
    return run(null, destinationWallet);
  }

  public boolean run(String destinationAddress) throws Exception {
    return run(destinationAddress, null);
  }

  private boolean run(String destinationAddress, Bip84Wallet destinationWallet) throws Exception {
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
        runAggregate(subsetUtxos, toAddress);
        success = true;

        log.info("Refreshing utxos...");
        samouraiApi.refreshUtxos();
      }
      round++;
    }
    return success;
  }

  private void runAggregate(List<UnspentResponse.UnspentOutput> postmixUtxos, String toAddress)
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
        new TxAggregateService(params)
            .txAggregate(spendFromOutPoints, spendFromAddresses, toAddress, feeSatPerByte);

    log.info("txAggregate:");
    log.info(txAggregate.toString());

    // broadcast
    log.info(" • Broadcasting TxAggregate...");
    pushTxService.pushTx(txAggregate);
  }
}
