package com.samourai.whirlpool.client.run;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.impl.Bip47Util;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IMixHandler;
import com.samourai.whirlpool.client.mix.handler.MixHandler;
import com.samourai.whirlpool.client.run.vpub.UnspentResponse;
import com.samourai.whirlpool.client.utils.MultiClientManager;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class RunMixVPub {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private WhirlpoolClientConfig config;
    private Bip47Util bip47Util = Bip47Util.getInstance();

    public RunMixVPub(WhirlpoolClientConfig config) {
        this.config = config;
    }

    public int runMix(List<UnspentResponse.UnspentOutput> mustMixUtxosPremix, BIP47Wallet bip47w, HD_Wallet bip84w, Pool pool, int paynymIndex) throws Exception {
        final int NB_CLIENTS = pool.getMixAnonymitySet();
        MultiClientManager multiClientManager = new MultiClientManager(NB_CLIENTS);

        // connect each client
        for (int i=0; i < NB_CLIENTS; i++) {
            UnspentResponse.UnspentOutput premixUtxo = mustMixUtxosPremix.remove(0);

            // key
            HD_Address premixAddress = bip84w.getAccountAt(RunVPub.ACCOUNT_PREMIX).getChain(RunVPub.CHAIN_PREMIX).getAddressAt(premixUtxo.computePathAddressIndex());
            String premixAddressBech32 = new SegwitAddress(premixAddress.getPubKey(), config.getNetworkParameters()).getBech32AsString();
            ECKey premixKey = premixAddress.getECKey();
            int nbMixs = 1;

            log.info("- mustMix[" + i + "]: utxo=" + premixUtxo + ", key=" + premixKey + ", address=" + premixAddressBech32+", path=" + premixAddress.toJSON().get("path") + ", paynymIndex=" + paynymIndex + " (" +premixUtxo.value + "sats)");
            WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(config);
            IMixHandler mixHandler = new MixHandler(premixKey, bip47w, paynymIndex, bip47Util);
            MixParams mixParams = new MixParams(premixUtxo.tx_hash, premixUtxo.tx_output_n, premixUtxo.value, mixHandler);
            WhirlpoolClientListener listener = multiClientManager.register(whirlpoolClient);
            whirlpoolClient.whirlpool(pool.getPoolId(), pool.getDenomination(), mixParams, nbMixs, listener);

            paynymIndex++;
        }
        multiClientManager.waitAllClientsSuccess();
        return paynymIndex;
    }

}
