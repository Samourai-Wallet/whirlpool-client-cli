package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bip69.BIP69InputComparator;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.TxUtil;
import com.samourai.whirlpool.client.utils.CliUtils;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxAggregateService {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final NetworkParameters params;
  private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  public TxAggregateService(NetworkParameters params) {
    this.params = params;
  }

  public Transaction txAggregate(
      List<TransactionOutPoint> spendFromOutpoints,
      List<HD_Address> spendFromAddresses,
      String toAddress,
      long feeSatPerByte)
      throws Exception {

    long inputsValue = spendFromOutpoints.stream().mapToLong(o -> o.getValue().getValue()).sum();

    Transaction tx = new Transaction(params);
    long minerFee = CliUtils.computeMinerFee(spendFromOutpoints.size(), 1, feeSatPerByte);
    long destinationValue = inputsValue - minerFee;

    // 1 output
    if (log.isDebugEnabled()) {
      log.debug("Tx out: address=" + toAddress + " (" + destinationValue + " sats)");
    }

    TransactionOutput output = bech32Util.getTransactionOutput(toAddress, destinationValue, params);
    tx.addOutput(output);

    // prepare N inputs
    List<TransactionInput> inputs = new ArrayList<>();
    Map<TransactionInput, ECKey> keysByInput = new HashMap<>();
    for (int i = 0; i < spendFromOutpoints.size(); i++) {
      TransactionOutPoint spendFromOutpoint = spendFromOutpoints.get(i);
      HD_Address spendFromAddress = spendFromAddresses.get(i);
      String spendFromAddressBech32 = bech32Util.toBech32(spendFromAddress, params);
      ECKey spendFromKey = spendFromAddress.getECKey();

      // final Script segwitPubkeyScript = ScriptBuilder.createP2WPKHOutputScript(spendFromKey);
      new Transaction(params);
      TransactionInput txInput =
          new TransactionInput(
              params, null, new byte[] {}, spendFromOutpoint, spendFromOutpoint.getValue());
      inputs.add(txInput);
      keysByInput.put(txInput, spendFromKey);
      if (log.isDebugEnabled()) {
        log.debug(
            "Tx in: address="
                + spendFromAddressBech32
                + ", utxo="
                + spendFromOutpoint
                + ", path="
                + spendFromAddress.toJSON().get("path")
                + " ("
                + spendFromOutpoint.getValue().getValue()
                + " sats)");
      }
    }

    // sort inputs & add
    Collections.sort(inputs, new BIP69InputComparator());
    for (TransactionInput ti : inputs) {
      tx.addInput(ti);
    }

    // sign inputs
    for (TransactionInput txInput : inputs) {
      ECKey spendFromKey = keysByInput.get(txInput);
      TransactionOutPoint txo = txInput.getOutpoint();
      int inputIndex =
          TxUtil.getInstance().findInputIndex(tx, txo.getHash().toString(), txo.getIndex());
      TxUtil.getInstance()
          .signInputSegwit(tx, inputIndex, spendFromKey, txInput.getValue().getValue(), params);
    }

    final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
    final String strTxHash = tx.getHashAsString();

    tx.verify();
    if (log.isDebugEnabled()) {
      log.debug("Tx hash: " + strTxHash);
      log.debug("Tx hex: " + hexTx + "\n");
    }
    return tx;
  }
}
