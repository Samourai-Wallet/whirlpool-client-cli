package com.samourai.whirlpool.client.run;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Chain;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.mix.handler.IMixHandler;
import com.samourai.whirlpool.client.utils.ClientUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class VPubMixHandler implements IMixHandler {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ECKey utxoKey;
    private HD_Chain receiveChain;
    private int receiveAddressIndex;
    private int incrementReceiveAddressIndex;
    private HD_Address receiveAddress;

    public VPubMixHandler(ECKey utxoKey, HD_Chain receiveChain, int receiveAddressIndex, int incrementReceiveAddressIndex) {
        this.utxoKey = utxoKey;
        this.receiveChain = receiveChain;
        this.receiveAddressIndex = receiveAddressIndex;
        this.incrementReceiveAddressIndex = incrementReceiveAddressIndex;
        this.receiveAddress = null;
    }

    @Override
    public String computeReceiveAddress(NetworkParameters params) throws Exception {
        this.receiveAddress = receiveChain.getAddressAt(receiveAddressIndex);

        String bech32Address = new SegwitAddress(receiveAddress.getPubKey(), params).getBech32AsString();
        log.info("receiveAddress="+bech32Address+", receiveKey="+receiveAddress.getECKey().getPrivateKeyAsWiF(params)+", path="+receiveAddress.toJSON().get("path"));
        return bech32Address;
    }

    @Override
    public void signTransaction(Transaction tx, int inputIndex, long spendAmount, NetworkParameters params) throws Exception {
        ClientUtils.signSegwitInput(tx, inputIndex, utxoKey, spendAmount, params);
    }

    @Override
    public String signMessage(String message) {
        return utxoKey.signMessage(message);
    }

    @Override
    public byte[] getPubkey() {
        return utxoKey.getPubKey();
    }

    @Override
    public IMixHandler computeMixHandlerForNextMix() {
        ECKey receiveKey = receiveAddress.getECKey();
        return new VPubMixHandler(receiveKey, receiveChain, receiveAddressIndex+incrementReceiveAddressIndex, incrementReceiveAddressIndex);
    }
}
