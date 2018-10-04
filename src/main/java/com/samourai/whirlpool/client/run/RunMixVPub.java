package com.samourai.whirlpool.client.run;

import com.samourai.wallet.bip47.rpc.impl.Bip47Util;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Chain;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IMixHandler;
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

    private static final int SLEEP_CONNECTING_CLIENTS_SECONDS = 10;

    public RunMixVPub(WhirlpoolClientConfig config) {
        this.config = config;
    }

    public void runMix(List<UnspentResponse.UnspentOutput> mustMixUtxosPremix, VpubWallet postmixWallet, Pool pool, SamouraiApi samouraiApi) throws Exception {
        final int NB_CLIENTS = pool.getMixAnonymitySet();
        MultiClientManager multiClientManager = new MultiClientManager();

        // connect each client
        for (int i=0; i < NB_CLIENTS; i++) {
            if (multiClientManager.isDone()) {
                break;
            }
            // pick last mustMix
            UnspentResponse.UnspentOutput premixUtxo = mustMixUtxosPremix.remove(mustMixUtxosPremix.size()-1);

            // input key from premix
            HD_Address premixAddress = postmixWallet.getBip84w().getAccountAt(RunVPubLoop.ACCOUNT_DEPOSIT_AND_PREMIX).getChain(RunVPubLoop.CHAIN_DEPOSIT_AND_PREMIX).getAddressAt(premixUtxo.computePathAddressIndex());
            String premixAddressBech32 = new SegwitAddress(premixAddress.getPubKey(), config.getNetworkParameters()).getBech32AsString();
            ECKey premixKey = premixAddress.getECKey();
            int nbMixs = 1;

            // receive address from postmix
            HD_Chain receiveChain = postmixWallet.getBip84w().getAccountAt(RunVPubLoop.ACCOUNT_POSTMIX).getChain(RunVPubLoop.CHAIN_POSTMIX);
            int receiveAddressIndex = postmixWallet.fetchAddress(samouraiApi).account_index;
            WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(config);
            IMixHandler mixHandler = new VPubMixHandler(premixKey, receiveChain, receiveAddressIndex, NB_CLIENTS);

            log.info(" => Connecting client " + i + ": mustMix, premixUtxo=" + premixUtxo + ", premixKey=" + premixKey + ", premixAddress=" + premixAddressBech32+", path=" + premixAddress.toJSON().get("path") + " (" +premixUtxo.value + "sats)");
            MixParams mixParams = new MixParams(premixUtxo.tx_hash, premixUtxo.tx_output_n, premixUtxo.value, mixHandler);
            WhirlpoolClientListener listener = multiClientManager.register(whirlpoolClient);
            whirlpoolClient.whirlpool(pool.getPoolId(), pool.getDenomination(), mixParams, nbMixs, listener);

            Thread.sleep(SLEEP_CONNECTING_CLIENTS_SECONDS*1000);
        }
        multiClientManager.waitDone();
    }

}
