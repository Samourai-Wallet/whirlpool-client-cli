package com.samourai.whirlpool.client;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.impl.Bip47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IMixHandler;
import com.samourai.whirlpool.client.mix.handler.MixHandler;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class RunWhirlpool {
    private static final Logger log = LoggerFactory.getLogger(RunWhirlpool.class);
    private static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

    private ApplicationArgs appArgs;

    public RunWhirlpool() {}

    public void run(ApplicationArgs appArgs, WhirlpoolClient whirlpoolClient, String poolId, long poolDenomination) throws Exception {
        this.appArgs = appArgs;

        // start mixing in a pool
        String utxoHash = appArgs.getUtxoHash();
        long utxoIdx = appArgs.getUtxoIdx();
        String utxoKey = appArgs.getUtxoKey();
        long utxoBalance = appArgs.getUtxoBalance();
        String seedWords = appArgs.getSeedWords();
        String seedPassphrase = appArgs.getSeedPassphrase();
        final int mixs = appArgs.getMixs();
        NetworkParameters params = appArgs.getNetworkParameters();

        // run
        CliListener listener = runWhirlpool(whirlpoolClient, poolId, poolDenomination, params, utxoHash, utxoIdx, utxoKey, utxoBalance, seedWords, seedPassphrase, mixs);
        listener.waitDone();
    }

    private CliListener runWhirlpool(WhirlpoolClient whirlpoolClient, String poolId, long poolDenomination, NetworkParameters params, String utxoHash, long utxoIdx, String utxoKey, long utxoBalance, String seedWords, String seedPassphrase, int mixs) throws Exception {
        // utxo key
        DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(params, utxoKey);
        ECKey ecKey = dumpedPrivateKey.getKey();

        // wallet
        InputStream wis = HD_Wallet.class.getResourceAsStream("/en_US.txt");
        List<String> seedWordsList = Arrays.asList(seedWords.split("\\s+"));
        MnemonicCode mc = new MnemonicCode(wis, BIP39_ENGLISH_SHA256);
        byte[] seed = mc.toEntropy(seedWordsList);

        // init BIP44 wallet
        HD_Wallet hdw = new HD_Wallet(44, mc, params, seed, seedPassphrase, 1);
        // init BIP47 wallet for input
        BIP47Wallet bip47w = new BIP47Wallet(47, mc, params, Hex.decode(hdw.getSeedHex()), hdw.getPassphrase(), 1);

        // whirlpool
        IMixHandler mixHandler = new MixHandler(ecKey, bip47w, appArgs.getPaynymIndex(), Bip47Util.getInstance());
        MixParams mixParams = new MixParams(utxoHash, utxoIdx, utxoBalance, mixHandler);
        CliListener listener = new CliListener();
        whirlpoolClient.whirlpool(poolId, poolDenomination, mixParams, mixs, listener);
        return listener;
    }

    private double satToBtc(long sat) {
        return sat / 100000000.0;
    }
}
