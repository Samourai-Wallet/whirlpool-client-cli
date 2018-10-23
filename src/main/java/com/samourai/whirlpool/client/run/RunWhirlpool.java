package com.samourai.whirlpool.client.run;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.impl.Bip47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.CliListener;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.*;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

public class RunWhirlpool {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public RunWhirlpool() {}

    public void run(WhirlpoolClient whirlpoolClient, Pool pool, NetworkParameters params, String utxoHash, long utxoIdx, String utxoKey, long utxoBalance, String seedWords, String seedPassphrase, int paynymIndex, int mixs) throws Exception {
        // utxo key
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(params, utxoKey);
        ECKey ecKey = dumpedPrivateKey.getKey();

        // wallet
        List<String> seedWordsList = Arrays.asList(seedWords.split("\\s+"));
        MnemonicCode mc = CliUtils.computeMnemonicCode();
        byte[] seed = mc.toEntropy(seedWordsList);

        // init BIP44 wallet
        HD_Wallet hdw = new HD_Wallet(44, mc, params, seed, seedPassphrase, 1);
        // init BIP47 wallet for input
        BIP47Wallet bip47w = new BIP47Wallet(47, mc, params, Hex.decode(hdw.getSeedHex()), hdw.getPassphrase(), 1);

        // whirlpool
        UtxoWithBalance utxo = new UtxoWithBalance(utxoHash, utxoIdx, utxoBalance);
        IPremixHandler premixHandler = new PremixHandler(utxo, ecKey);
        IPostmixHandler postmixHandler = new PostmixHandler(bip47w, paynymIndex, Bip47Util.getInstance());
        MixParams mixParams = new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);
        CliListener listener = new CliListener();
        whirlpoolClient.whirlpool(mixParams, mixs, listener);
        listener.waitDone();
    }
}
