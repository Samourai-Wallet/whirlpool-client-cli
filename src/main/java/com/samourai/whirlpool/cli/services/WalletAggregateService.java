package com.samourai.whirlpool.cli.services;

import com.samourai.wallet.api.backend.BackendApi;
import com.samourai.wallet.api.backend.MinerFeeTarget;
import com.samourai.wallet.api.backend.beans.UnspentResponse;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
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
  private static final int AGGREGATED_UTXOS_PER_TX = 600;
  private static final FormatsUtilGeneric formatUtils = FormatsUtilGeneric.getInstance();

  private NetworkParameters params;
  private CliConfig cliConfig;
  private Bech32UtilGeneric bech32Util;
  private TxAggregateService txAggregateService;

  public WalletAggregateService(
      NetworkParameters params,
      CliConfig cliConfig,
      Bech32UtilGeneric bech32Util,
      TxAggregateService txAggregateService) {
    this.params = params;
    this.cliConfig = cliConfig;
    this.bech32Util = bech32Util;
    this.txAggregateService = txAggregateService;
  }

  public boolean toWallet(
      Bip84ApiWallet sourceWallet,
      Bip84Wallet destinationWallet,
      int feeSatPerByte,
      BackendApi backendApi)
      throws Exception {
    return doAggregate(sourceWallet, null, destinationWallet, feeSatPerByte, backendApi);
  }

  public boolean toAddress(
      Bip84ApiWallet sourceWallet, String destinationAddress, CliWallet cliWallet)
      throws Exception {
    if (!formatUtils.isTestNet(cliConfig.getServer().getParams())) {
      throw new NotifiableException(
          "aggregate toAddress is disabled on mainnet for security reasons.");
    }

    int feeSatPerByte = cliWallet.getFee(MinerFeeTarget.BLOCKS_2);
    BackendApi backendApi = cliWallet.getConfig().getBackendApi();
    return doAggregate(sourceWallet, destinationAddress, null, feeSatPerByte, backendApi);
  }

  private boolean doAggregate(
      Bip84ApiWallet sourceWallet,
      String destinationAddress,
      Bip84Wallet destinationWallet,
      int feeSatPerByte,
      BackendApi backendApi)
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
      if (!subsetUtxos.isEmpty()) {
        String toAddress = destinationAddress;
        if (toAddress == null) {
          toAddress = bech32Util.toBech32(destinationWallet.getNextAddress(), params);
        }

        log.info("Aggregating " + subsetUtxos.size() + " utxos (pass #" + round + ")");
        txAggregate(sourceWallet, subsetUtxos, toAddress, feeSatPerByte, backendApi);
        success = true;

        ClientUtils.sleepRefreshUtxos(cliConfig.getServer().getParams());
      }
      round++;
    }
    return success;
  }

  private void txAggregate(
      Bip84ApiWallet sourceWallet,
      List<UnspentResponse.UnspentOutput> postmixUtxos,
      String toAddress,
      int feeSatPerByte,
      BackendApi backendApi)
      throws Exception {
    List<TransactionOutPoint> spendFromOutPoints = new ArrayList<>();
    List<HD_Address> spendFromAddresses = new ArrayList<>();

    // spend
    for (UnspentResponse.UnspentOutput utxo : postmixUtxos) {
      spendFromOutPoints.add(utxo.computeOutpoint(params));
      spendFromAddresses.add(sourceWallet.getAddressAt(utxo));
    }

    // tx
    Transaction txAggregate =
        txAggregateService.txAggregate(
            spendFromOutPoints, spendFromAddresses, toAddress, feeSatPerByte);

    log.info("txAggregate:");
    log.info(txAggregate.toString());

    // broadcast
    log.info(" • Broadcasting TxAggregate...");
    String txHex = ClientUtils.getTxHex(txAggregate);
    backendApi.pushTx(txHex);
  }

  public boolean consolidateWallet(CliWallet cliWallet) throws Exception {
    if (!formatUtils.isTestNet(cliConfig.getServer().getParams())) {
      log.warn("You should NOT consolidateWallet on mainnet for privacy reasons!");
    }

    Bip84ApiWallet depositWallet = cliWallet.getWalletDeposit();
    Bip84ApiWallet premixWallet = cliWallet.getWalletPremix();
    Bip84ApiWallet postmixWallet = cliWallet.getWalletPostmix();

    int feeSatPerByte = cliWallet.getFee(MinerFeeTarget.BLOCKS_2);
    BackendApi backendApi = cliWallet.getConfig().getBackendApi();

    log.info(" • Consolidating postmix -> deposit...");
    toWallet(postmixWallet, depositWallet, feeSatPerByte, backendApi);

    log.info(" • Consolidating premix -> deposit...");
    toWallet(premixWallet, depositWallet, feeSatPerByte, backendApi);

    if (depositWallet.fetchUtxos().size() < 2) {
      log.info(" • Consolidating deposit... nothing to aggregate.");
      return false;
    }
    log.info(" • Consolidating deposit...");
    boolean success = toWallet(depositWallet, depositWallet, feeSatPerByte, backendApi);
    return success;
  }
}
