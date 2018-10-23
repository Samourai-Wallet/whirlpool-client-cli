package com.samourai.whirlpool.client.run;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Chain;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class VPubPostmixHandler implements IPostmixHandler {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private HD_Chain receiveChain;
    private int receiveAddressIndex;
    private HD_Address receiveAddress;

    public VPubPostmixHandler(HD_Chain receiveChain, int receiveAddressIndex) {
        this.receiveChain = receiveChain;
        this.receiveAddressIndex = receiveAddressIndex;
        this.receiveAddress = null;
    }

    @Override
    public String computeReceiveAddress(NetworkParameters params) throws Exception {
        this.receiveAddress = receiveChain.getAddressAt(receiveAddressIndex);
        receiveAddressIndex++;

        String bech32Address = new SegwitAddress(receiveAddress.getPubKey(), params).getBech32AsString();
        log.info("receiveAddress="+bech32Address+", receiveKey="+receiveAddress.getECKey().getPrivateKeyAsWiF(params)+", path="+receiveAddress.toJSON().get("path"));
        return bech32Address;
    }

    @Override
    public IPremixHandler computeNextPremixHandler(UtxoWithBalance receiveUtxo) {
        ECKey receiveKey = receiveAddress.getECKey();
        return new PremixHandler(receiveUtxo, receiveKey);
    }
}
