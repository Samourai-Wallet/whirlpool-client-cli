package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.wallet.bip47.rpc.impl.Bip47Util;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Chain;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
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

    private static final int SLEEP_CONNECTING_CLIENTS_SECONDS = 30;

    public RunMixVPub(WhirlpoolClientConfig config) {
        this.config = config;
    }

    public void runMix(List<UnspentResponse.UnspentOutput> mustMixUtxosPremix, VpubWallet postmixWallet, Pool pool, SamouraiApi samouraiApi) throws Exception {
        final int NB_CLIENTS = pool.getMixAnonymitySet();
        MultiClientManager multiClientManager = new MultiClientManager();

        // fetch receiveAddress index
        int receiveAddressIndex = postmixWallet.fetchAddress(samouraiApi).account_index;
        HD_Chain receiveChain = postmixWallet.getBip84w().getAccountAt(RunVPubLoop.ACCOUNT_POSTMIX).getChain(RunVPubLoop.CHAIN_POSTMIX);
        IPostmixHandler postmixHandler = new VPubPostmixHandler(receiveChain, receiveAddressIndex);

        // connect each client
        for (int i=0; i < NB_CLIENTS; i++) {
            if (multiClientManager.isDone()) {
                break;
            }
            // pick last mustMix
            UnspentResponse.UnspentOutput premixUtxo = mustMixUtxosPremix.remove(mustMixUtxosPremix.size()-1);
            UtxoWithBalance premixUtxoWithBalance = new UtxoWithBalance(premixUtxo.tx_hash, premixUtxo.tx_output_n, premixUtxo.value);

            // input key from premix
            HD_Address premixAddress = postmixWallet.getBip84w().getAccountAt(RunVPubLoop.ACCOUNT_DEPOSIT_AND_PREMIX).getChain(RunVPubLoop.CHAIN_DEPOSIT_AND_PREMIX).getAddressAt(premixUtxo.computePathAddressIndex());
            String premixAddressBech32 = new SegwitAddress(premixAddress.getPubKey(), config.getNetworkParameters()).getBech32AsString();
            ECKey premixKey = premixAddress.getECKey();
            IPremixHandler premixHandler = new PremixHandler(premixUtxoWithBalance, premixKey);

            // receive address from postmix

            // one config / StompClient per client
            WhirlpoolClientConfig clientConfig = new WhirlpoolClientConfig(config);
            clientConfig.setStompClient(new JavaStompClient());
            WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(clientConfig);

            log.info(" => Connecting client #" + (i+1) + ": mustMix, premixUtxo=" + premixUtxo + ", premixKey=" + premixKey.getPrivateKeyAsWiF(config.getNetworkParameters()) + ", premixAddress=" + premixAddressBech32+", path=" + premixAddress.toJSON().get("path") + " (" +premixUtxo.value + "sats)");
            MixParams mixParams = new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);
            WhirlpoolClientListener listener = multiClientManager.register(whirlpoolClient);
            whirlpoolClient.whirlpool(mixParams, 1, listener);

            Thread.sleep(SLEEP_CONNECTING_CLIENTS_SECONDS*1000);
        }
        multiClientManager.waitDone();
    }

}
