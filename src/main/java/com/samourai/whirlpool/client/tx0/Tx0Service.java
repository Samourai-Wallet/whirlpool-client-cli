package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.wallet.util.FormatsUtilGeneric;
import com.samourai.whirlpool.client.utils.Bip84Wallet;
import com.samourai.whirlpool.client.utils.CliUtils;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tx0Service {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  private final NetworkParameters params;

  public Tx0Service(NetworkParameters params) {
    this.params = params;
  }

  public Tx0 tx0(
      byte[] spendFromPrivKey,
      TransactionOutPoint spendFromOutpoint,
      int nbOutputs,
      Bip84Wallet depositAndPremixWallet,
      long destinationValue,
      long feeSatPerByte,
      String xpubFee,
      long fee,
      String feePaymentCode,
      int feeIndice)
      throws Exception {

    long spendFromBalance = spendFromOutpoint.getValue().getValue();

    Tx0 tx0Result = new Tx0();

    //
    // tx0
    //

    //
    // make tx:
    // 5 spendTo outputs
    // SW fee
    // change
    // OP_RETURN
    //
    List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();
    Transaction tx = new Transaction(params);

    //
    // premix outputs
    //
    for (int j = 0; j < nbOutputs; j++) {
      // send to PREMIX
      HD_Address toAddress = depositAndPremixWallet.getNextAddress();
      String toAddressBech32 = bech32Util.toBech32(toAddress, params);
      ECKey toAddressKey = toAddress.getECKey();
      tx0Result.getToKeys().put(toAddressBech32, toAddressKey);
      if (log.isDebugEnabled()) {
        log.debug(
            "Tx0 out (premix): address="
                + toAddressBech32
                + ", key="
                + toAddressKey.getPrivateKeyAsWiF(params)
                + ", path="
                + toAddress.toJSON().get("path")
                + " ("
                + destinationValue
                + " sats)");
      }

      TransactionOutput txOutSpend =
          bech32Util.getTransactionOutput(toAddressBech32, destinationValue, params);
      outputs.add(txOutSpend);
    }

    byte[] opReturnValue =
        WhirlpoolProtocol.getWhirlpoolFee()
            .encode(feeIndice, feePaymentCode, params, spendFromPrivKey, spendFromOutpoint);

    // fee estimation: n outputs + change + fee + OP_RETURN
    long totalBytes =
        CliUtils.estimateTxBytes(1, nbOutputs + 2) + CliUtils.estimateOpReturnBytes(opReturnValue);
    if (log.isDebugEnabled()) {
      log.debug("totalBytes=" + totalBytes + "b");
    }
    long tx0MinerFee = CliUtils.computeMinerFee(totalBytes, feeSatPerByte);
    long changeValue = spendFromBalance - (destinationValue * nbOutputs) - fee - tx0MinerFee;

    //
    // 1 change output
    //
    HD_Address changeAddress = depositAndPremixWallet.getNextAddress();
    String changeAddressBech32 = bech32Util.toBech32(changeAddress, params);
    TransactionOutput txChange =
        bech32Util.getTransactionOutput(changeAddressBech32, changeValue, params);
    outputs.add(txChange);
    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0 out (change): address="
              + changeAddressBech32
              + ", path="
              + changeAddress.toJSON().get("path")
              + " ("
              + changeValue
              + " sats)");
    }

    // derive fee address
    DeterministicKey mKey = FormatsUtilGeneric.getInstance().createMasterPubKeyFromXPub(xpubFee);
    DeterministicKey cKey =
        HDKeyDerivation.deriveChildKey(
            mKey, new ChildNumber(0, false)); // assume external/receive chain
    DeterministicKey adk = HDKeyDerivation.deriveChildKey(cKey, new ChildNumber(feeIndice, false));
    ECKey feePubkey = ECKey.fromPublicOnly(adk.getPubKey());
    String feeAddressBech32 = bech32Util.toBech32(feePubkey.getPubKey(), params);

    TransactionOutput txSWFee = bech32Util.getTransactionOutput(feeAddressBech32, fee, params);
    outputs.add(txSWFee);
    if (log.isDebugEnabled()) {
      log.debug("Tx0 out (fee): address=" + feeAddressBech32 + " (" + fee + " sats)");
    }

    // add OP_RETURN output
    Script op_returnOutputScript =
        new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(opReturnValue).build();
    TransactionOutput txFeeOutput =
        new TransactionOutput(params, null, Coin.valueOf(0L), op_returnOutputScript.getProgram());
    outputs.add(txFeeOutput);
    if (log.isDebugEnabled()) {
      log.debug("Tx0 out (OP_RETURN): feeIndice=" + feeIndice);
    }

    // all outputs
    Collections.sort(outputs, new BIP69OutputComparator());
    for (TransactionOutput to : outputs) {
      tx.addOutput(to);
    }

    // input
    ECKey spendFromKey = ECKey.fromPrivate(spendFromPrivKey);

    final Script segwitPubkeyScript = ScriptBuilder.createP2WPKHOutputScript(spendFromKey);
    tx.addSignedInput(spendFromOutpoint, segwitPubkeyScript, spendFromKey);
    if (log.isDebugEnabled()) {
      log.debug(
          "Tx0 in: utxo="
              + spendFromOutpoint
              + " ("
              + spendFromOutpoint.getValue().getValue()
              + " sats), key="
              + spendFromKey.getPrivateKeyAsWiF(params));
      log.debug("Tx0 fee: " + tx0MinerFee + " sats");
    }

    final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
    final String strTxHash = tx.getHashAsString();

    tx.verify();
    // System.out.println(tx);
    if (log.isDebugEnabled()) {
      log.debug("Tx0 hash: " + strTxHash);
      log.debug("Tx0 hex: " + hexTx);
      long feePrice = tx0MinerFee / tx.getVirtualTransactionSize();
      log.debug("Tx0 size: " + tx.getVirtualTransactionSize() + "b, feePrice=" + feePrice + "s/b");
    }

    for (TransactionOutput to : tx.getOutputs()) {
      tx0Result
          .getToUTXO()
          .put(Hex.toHexString(to.getScriptBytes()), strTxHash + "-" + to.getIndex());
    }

    tx0Result.setTx(tx);
    return tx0Result;
  }
}
