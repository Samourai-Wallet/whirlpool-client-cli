package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.rpc.client.RpcClientService;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Service;
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

public class RunTx0VPub {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private NetworkParameters params;
  private SamouraiApi samouraiApi;
  private Optional<RpcClientService> rpcClientService;
  private Bip84ApiWallet depositAndPremixWallet;

  private static final String XPUB_SAMOURAI_FEES =
      "vpub5YS8pQgZKVbrSn9wtrmydDWmWMjHrxL2mBCZ81BDp7Z2QyCgTLZCrnBprufuoUJaQu1ZeiRvUkvdQTNqV6hS96WbbVZgweFxYR1RXYkBcKt";
  private static final long SAMOURAI_FEES = 10000; // TODO

  public RunTx0VPub(
      NetworkParameters params,
      SamouraiApi samouraiApi,
      Optional<RpcClientService> rpcClientService,
      Bip84ApiWallet depositAndPremixWallet) {
    this.params = params;
    this.samouraiApi = samouraiApi;
    this.rpcClientService = rpcClientService;
    this.depositAndPremixWallet = depositAndPremixWallet;
  }

  public Tx0 runTx0(Pool pool, int nbOutputs) throws Exception {
    List<UnspentResponse.UnspentOutput> utxos = depositAndPremixWallet.fetchUtxos();
    if (utxos.isEmpty()) {
      throw new NotifiableException("No utxo found from VPub.");
    }

    log.info("Found " + utxos.size() + " utxo from premix:");
    CliUtils.printUtxos(utxos);
    return runTx0(utxos, pool, nbOutputs);
  }

  public Tx0 runTx0(List<UnspentResponse.UnspentOutput> utxos, Pool pool, int nbOutputs)
      throws Exception {

    // fetch spend address info
    long destinationValue = computeDestinationValue(pool);

    // find utxo to spend Tx0 from
    long spendFromBalanceMin = nbOutputs * (destinationValue + SAMOURAI_FEES);
    List<UnspentResponse.UnspentOutput> tx0SpendFroms =
        utxos
            .stream()
            .filter(utxo -> utxo.value >= spendFromBalanceMin)
            .collect(Collectors.toList());

    if (tx0SpendFroms.isEmpty()) {
      throw new Exception("ERROR: No utxo available to spend Tx0 from");
    }
    log.info("Found " + tx0SpendFroms.size() + " utxos to use as Tx0 input");
    CliUtils.printUtxos(tx0SpendFroms);

    UnspentResponse.UnspentOutput tx0SpendFrom = tx0SpendFroms.get(0);
    Tx0 tx0 = runTx0(tx0SpendFrom, destinationValue, nbOutputs);
    return tx0;
  }

  private Tx0 runTx0(UnspentResponse.UnspentOutput spendFrom, long destinationValue, int nbOutputs)
      throws Exception {

    // spend from
    TransactionOutPoint spendFromOutpoint = spendFrom.computeOutpoint(params);
    HD_Address spendFromAddress =
        depositAndPremixWallet.getAddressAt(spendFrom);

    // run tx0
    int feeSatPerByte = samouraiApi.fetchFees();
    Tx0 tx0 =
        new Tx0Service(params)
            .tx0(
                spendFromAddress,
                spendFromOutpoint,
                nbOutputs,
                depositAndPremixWallet,
                destinationValue,
                feeSatPerByte,
                XPUB_SAMOURAI_FEES,
                SAMOURAI_FEES);

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
