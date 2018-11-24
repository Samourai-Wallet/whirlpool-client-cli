package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.Bip84ApiWallet;
import com.samourai.whirlpool.client.utils.CliUtils;
import com.samourai.whirlpool.client.utils.indexHandler.IIndexHandler;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunTx0 {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private Optional<RpcClientService> rpcClientService;
  private Bip84ApiWallet depositAndPremixWallet;

  private static final String FEE_XPUB =
      "vpub5YS8pQgZKVbrSn9wtrmydDWmWMjHrxL2mBCZ81BDp7Z2QyCgTLZCrnBprufuoUJaQu1ZeiRvUkvdQTNqV6hS96WbbVZgweFxYR1RXYkBcKt";
  public static final long FEE_VALUE = 10000; // TODO

  public RunTx0(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      Optional<RpcClientService> rpcClientService,
      Bip84ApiWallet depositAndPremixWallet) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.rpcClientService = rpcClientService;
    this.depositAndPremixWallet = depositAndPremixWallet;
  }

  public Tx0 runTx0(Pool pool, int nbOutputs, String feePaymentCode, IIndexHandler feeIndexHandler)
      throws Exception {
    List<UnspentResponse.UnspentOutput> utxos = depositAndPremixWallet.fetchUtxos();
    if (utxos.isEmpty()) {
      throw new NotifiableException("No utxo found from premix.");
    }

    if (log.isDebugEnabled()) {
      log.debug("Found " + utxos.size() + " utxo from premix:");
      CliUtils.printUtxos(utxos);
    }

    // fetch spend address info
    long destinationValue = computeDestinationValue(pool);

    // find utxo to spend Tx0 from
    long spendFromBalanceMin = nbOutputs * (destinationValue + FEE_VALUE);
    List<UnspentResponse.UnspentOutput> tx0SpendFroms =
        utxos
            .stream()
            .filter(utxo -> utxo.value >= spendFromBalanceMin)
            .collect(Collectors.toList());

    if (tx0SpendFroms.isEmpty()) {
      throw new Exception("ERROR: No utxo available to spend Tx0 from");
    }
    if (log.isDebugEnabled()) {
      log.debug("Found " + tx0SpendFroms.size() + " utxos to use as Tx0 input");
      CliUtils.printUtxos(tx0SpendFroms);
    }

    UnspentResponse.UnspentOutput tx0SpendFrom = tx0SpendFroms.get(0);
    int feeIndice = feeIndexHandler.getAndIncrement();
    Tx0 tx0 = runTx0(tx0SpendFrom, destinationValue, nbOutputs, feePaymentCode, feeIndice);
    return tx0;
  }

  private Tx0 runTx0(
      UnspentResponse.UnspentOutput spendFrom,
      long destinationValue,
      int nbOutputs,
      String feePaymentCode,
      int feeIndice)
      throws Exception {

    // spend from
    TransactionOutPoint spendFromOutpoint = spendFrom.computeOutpoint(params);
    byte[] spendFromPrivKey =
        depositAndPremixWallet.getAddressAt(spendFrom).getECKey().getPrivKeyBytes();

    // run tx0
    int feeSatPerByte = samouraiApi.fetchFees();
    Tx0 tx0 =
        new Tx0Service(params)
            .tx0(
                spendFromPrivKey,
                spendFromOutpoint,
                nbOutputs,
                depositAndPremixWallet,
                destinationValue,
                feeSatPerByte,
                FEE_XPUB,
                FEE_VALUE,
                feePaymentCode,
                feeIndice);

    log.info("Tx0:");
    log.info(tx0.getTx().toString());

    // broadcast
    log.info(" • Broadcasting Tx0...");
    CliUtils.broadcastOrNotify(rpcClientService, tx0.getTx());
    return tx0;
  }

  private long computeDestinationValue(Pool pool) {
    // compute minerFeePerMustmix
    int feeSatPerByte = samouraiApi.fetchFees();
    long txFeesEstimate =
        CliUtils.computeMinerFee(
            pool.getMixAnonymitySet(), pool.getMixAnonymitySet(), feeSatPerByte);
    long minerFeePerMustmix = txFeesEstimate / pool.getMixAnonymitySet();
    long destinationValue = pool.getDenomination() + minerFeePerMustmix;

    // make sure destinationValue is acceptable for pool
    long balanceMin =
        WhirlpoolProtocol.computeInputBalanceMin(
            pool.getDenomination(), false, pool.getMinerFeeMin());
    long balanceMax =
        WhirlpoolProtocol.computeInputBalanceMax(
            pool.getDenomination(), false, pool.getMinerFeeMax());
    destinationValue = Math.min(destinationValue, balanceMax);
    destinationValue = Math.max(destinationValue, balanceMin);

    if (log.isDebugEnabled()) {
      log.debug(
          "destinationValue="
              + destinationValue
              + ", minerFeePerMustmix="
              + minerFeePerMustmix
              + ", txFeesEstimate="
              + txFeesEstimate);
    }
    return destinationValue;
  }
}
