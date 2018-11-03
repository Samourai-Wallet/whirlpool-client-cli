package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.CliUtils;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
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
      HD_Address toAddress,
      long feeSatPerByte)
      throws Exception {

    long inputsValue = spendFromOutpoints.stream().mapToLong(o -> o.getValue().getValue()).sum();

    Transaction tx = new Transaction(params);
    long minerFee = CliUtils.computeMinerFee(spendFromOutpoints.size(), 1, feeSatPerByte);
    long destinationValue = inputsValue - minerFee;

    // 1 output
    String toAddressBech32 = new SegwitAddress(toAddress.getPubKey(), params).getBech32AsString();
    ECKey toAddressKey = toAddress.getECKey();
    log.info(
        "Tx out: address="
            + toAddressBech32
            + ", key="
            + toAddressKey.getPrivateKeyAsWiF(params)
            + ", path="
            + toAddress.toJSON().get("path")
            + " ("
            + destinationValue
            + " sats)");

    TransactionOutput output =
        bech32Util.getTransactionOutput(toAddressBech32, destinationValue, params);
    tx.addOutput(output);

    // N inputs
    for (int i = 0; i < spendFromOutpoints.size(); i++) {
      TransactionOutPoint spendFromOutpoint = spendFromOutpoints.get(i);
      HD_Address spendFromAddress = spendFromAddresses.get(i);
      String spendFromAddressBech32 =
          new SegwitAddress(spendFromAddress.getPubKey(), params).getBech32AsString();
      ECKey spendFromKey = spendFromAddress.getECKey();

      final Script segwitPubkeyScript = ScriptBuilder.createP2WPKHOutputScript(spendFromKey);
      tx.addSignedInput(spendFromOutpoint, segwitPubkeyScript, spendFromKey);
      log.info(
          "Tx in: address="
              + spendFromAddressBech32
              + ", utxo="
              + spendFromOutpoint
              + ", key="
              + spendFromKey.getPrivateKeyAsWiF(params)
              + ", path="
              + spendFromAddress.toJSON().get("path")
              + " ("
              + spendFromOutpoint.getValue().getValue()
              + " sats)");
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "minerFee="
              + minerFee
              + "sats, feeSatPerByte="
              + +feeSatPerByte);
    }

    final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
    final String strTxHash = tx.getHashAsString();

    tx.verify();
    // System.out.println(tx);
    log.info("Tx hash: " + strTxHash);
    log.info("Tx hex: " + hexTx + "\n");

    return tx;
  }
}
