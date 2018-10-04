package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.bip69.BIP69OutputComparator;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Chain;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.wallet.segwit.bech32.Bech32Segwit;
import com.samourai.wallet.util.FormatsUtilGeneric;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tx0Service {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final NetworkParameters params;

    private static final int TX0_BYTES_PER_OUTPUT = 200; // average b/output

    public Tx0Service(NetworkParameters params) {
        this.params = params;
    }

    public Tx0 tx0(HD_Address spendFromAddress, TransactionOutPoint spendFromOutpoint,
                   int nbOutputs, HD_Chain destinationChain, long destinationValue, int destinationIndex,
                   HD_Address changeAddress, long feeSatPerByte, String xpubSamouraiFees, long samouraiFees) throws Exception {
        boolean isTestnet = FormatsUtilGeneric.getInstance().isTestNet(params);
        int samouraiFeeIdx  = 0; // TODO address index, in prod get index from Samourai API

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
        for(int j = 0; j < nbOutputs; j++)   {
            // send to PREMIX
            HD_Address toAddress = destinationChain.getAddressAt(destinationIndex);
            String toAddressBech32 = new SegwitAddress(toAddress.getPubKey(), params).getBech32AsString();
            ECKey toAddressKey = toAddress.getECKey();
            tx0Result.getToKeys().put(toAddressBech32, toAddressKey);
            log.info("Tx0 out (premix): address="  + toAddressBech32 + ", key=" + toAddressKey.getPrivateKeyAsWiF(params) + ", path=" + toAddress.toJSON().get("path") + " (" + destinationValue + " sats)");

            Pair<Byte, byte[]> pair = Bech32Segwit.decode(isTestnet ? "tb" : "bc", toAddressBech32);
            byte[] scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());

            TransactionOutput txOutSpend = new TransactionOutput(params, null, Coin.valueOf(destinationValue), scriptPubKey);
            outputs.add(txOutSpend);
            destinationIndex++;
        }

        long tx0MinerFee = nbOutputs * feeSatPerByte * TX0_BYTES_PER_OUTPUT;
        if (log.isDebugEnabled()) {
            log.debug("tx0MinerFee=" + tx0MinerFee + "sats ("+nbOutputs+" * "+feeSatPerByte+"/b * "+TX0_BYTES_PER_OUTPUT+")");
        }
        long changeValue = spendFromBalance - (destinationValue * nbOutputs) - samouraiFees - tx0MinerFee;

        //
        // 1 change output
        //
        String changeAddressBech32 = new SegwitAddress(changeAddress.getPubKey(), params).getBech32AsString();
        Pair<Byte, byte[]> pair = Bech32Segwit.decode(isTestnet ? "tb" : "bc", changeAddressBech32);
        byte[] _scriptPubKey = Bech32Segwit.getScriptPubkey(pair.getLeft(), pair.getRight());
        TransactionOutput txChange = new TransactionOutput(params, null, Coin.valueOf(changeValue), _scriptPubKey);
        outputs.add(txChange);
        log.info("Tx0 out (change): address=" + changeAddressBech32 + ", path=" + changeAddress.toJSON().get("path") + " (" + changeValue + " sats)");

        // derive fee address
        DeterministicKey mKey = FormatsUtilGeneric.getInstance().createMasterPubKeyFromXPub(xpubSamouraiFees);
        DeterministicKey cKey = HDKeyDerivation.deriveChildKey(mKey, new ChildNumber(0, false)); // assume external/receive chain
        DeterministicKey adk = HDKeyDerivation.deriveChildKey(cKey, new ChildNumber(samouraiFeeIdx, false));
        ECKey samouraiFeePubkey = ECKey.fromPublicOnly(adk.getPubKey());
        String samouraiFeeAddressBech32 = new SegwitAddress(samouraiFeePubkey.getPubKey(), params).getBech32AsString();

        Script outputScript = ScriptBuilder.createP2WPKHOutputScript(samouraiFeePubkey);
        TransactionOutput txSWFee = new TransactionOutput(params, null, Coin.valueOf(samouraiFees), outputScript.getProgram());
        outputs.add(txSWFee);
        log.info("Tx0 out (samouraiFees): address=" + samouraiFeeAddressBech32 + " (" + samouraiFees + " sats)");

        // add OP_RETURN output
        byte[] idxBuf = ByteBuffer.allocate(4).putInt(samouraiFeeIdx).array();
        Script op_returnOutputScript = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data(idxBuf).build();
        TransactionOutput txFeeOutput = new TransactionOutput(params, null, Coin.valueOf(0L), op_returnOutputScript.getProgram());
        outputs.add(txFeeOutput);
        log.info("Tx0 out (OP_RETURN): samouraiFeeIdx=" + samouraiFeeIdx);

        // all outputs
        Collections.sort(outputs, new BIP69OutputComparator());
        for(TransactionOutput to : outputs) {
            tx.addOutput(to);
        }

        // input
        String spendFromAddressBech32 = new SegwitAddress(spendFromAddress.getPubKey(), params).getBech32AsString();
        ECKey spendFromKey = spendFromAddress.getECKey();

        final Script segwitPubkeyScript = ScriptBuilder.createP2WPKHOutputScript(spendFromKey);
        tx.addSignedInput(spendFromOutpoint, segwitPubkeyScript, spendFromKey);
        log.info("Tx0 in: address=" + spendFromAddressBech32 + ", utxo=" + spendFromOutpoint+ ", key=" + spendFromKey.getPrivateKeyAsWiF (params) + ", path=" + spendFromAddress.toJSON().get("path")  + " (" + spendFromOutpoint.getValue().getValue() + " sats)");
        log.info("Tx0 fee: " + tx0MinerFee+" sats");

        final String hexTx = new String(Hex.encode(tx.bitcoinSerialize()));
        final String strTxHash = tx.getHashAsString();

        tx.verify();
        //System.out.println(tx);
        log.info("Tx0 hash: " + strTxHash);
        log.info("Tx0 hex: " + hexTx + "\n");

        for(TransactionOutput to : tx.getOutputs())   {
            tx0Result.getToUTXO().put(Hex.toHexString(to.getScriptBytes()), strTxHash + "-" + to.getIndex());
        }

        tx0Result.setTx(tx);
        return tx0Result;
    }

}
