package com.samourai.whirlpool.client.run;

import com.samourai.api.beans.UnspentResponse;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.tor.client.JavaTorClient;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import com.samourai.whirlpool.client.utils.Bip84Wallet;
import com.samourai.whirlpool.client.utils.MultiClientManager;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMixWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private WhirlpoolClientConfig config;
  private Optional<JavaTorClient> torClient;
  private Bip84Wallet premixWallet;
  private Bip84Wallet postmixWallet;
  private int clientDelay;
  private int nbClients;
  private Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  public RunMixWallet(
      WhirlpoolClientConfig config,
      Optional<JavaTorClient> torClient,
      Bip84Wallet premixWallet,
      Bip84Wallet postmixWallet,
      int clientDelay,
      int nbClients) {
    this.config = config;
    this.torClient = torClient;
    this.premixWallet = premixWallet;
    this.postmixWallet = postmixWallet;
    this.clientDelay = clientDelay;
    this.nbClients = nbClients;
  }

  public boolean runMix(List<UnspentResponse.UnspentOutput> mustMixUtxosPremix, Pool pool)
      throws Exception {
    MultiClientManager multiClientManager = new MultiClientManager();

    // connect each client
    for (int i = 0; i < nbClients; i++) {
      // pick last mustMix
      UnspentResponse.UnspentOutput premixUtxo =
          mustMixUtxosPremix.remove(mustMixUtxosPremix.size() - 1);

      // one config / StompClient per client
      WhirlpoolClientConfig clientConfig = new WhirlpoolClientConfig(config);
      clientConfig.setStompClient(new JavaStompClient(torClient));
      WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(clientConfig);
      WhirlpoolClientListener listener = multiClientManager.register(whirlpoolClient);

      // start in a new thread as we may wait for TOR connexion
      final int iClient = i;
      new Thread(
              new Runnable() {
                @Override
                public void run() {
                  if (torClient.isPresent()) {
                    // for N clients we need N TOR connexions ready to register outputs
                    torClient.get().waitConnexionReady(iClient + 1);
                  }
                  runMixClient(whirlpoolClient, listener, premixUtxo, iClient, pool);
                }
              })
          .start();

      if (clientDelay > 0) {
        log.info("Waiting client-delay: " + (clientDelay / 1000) + "s");
        Thread.sleep(clientDelay);
      }
    }

    // quit as soon as we mixed at least 1 utxo
    boolean success = multiClientManager.waitDone(1, 1);
    multiClientManager.exit();
    return success;
  }

  private WhirlpoolClient runMixClient(
      WhirlpoolClient whirlpoolClient,
      WhirlpoolClientListener listener,
      UnspentResponse.UnspentOutput premixUtxo,
      int i,
      Pool pool) {
    UtxoWithBalance premixUtxoWithBalance =
        new UtxoWithBalance(premixUtxo.tx_hash, premixUtxo.tx_output_n, premixUtxo.value);

    // input key from premix
    HD_Address premixAddress = premixWallet.getAddressAt(premixUtxo);
    String premixAddressBech32 = bech32Util.toBech32(premixAddress, config.getNetworkParameters());
    ECKey premixKey = premixAddress.getECKey();
    IPremixHandler premixHandler = new PremixHandler(premixUtxoWithBalance, premixKey);
    IPostmixHandler postmixHandler = new Bip84PostmixHandler(postmixWallet);
    MixParams mixParams =
        new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);

    if (log.isDebugEnabled()) {
      log.debug(
          " • Connecting client "
              + (i + 1)
              + "/"
              + nbClients
              + ": mustMix, premixUtxo="
              + premixUtxo
              + ", premixAddress="
              + premixAddressBech32
              + ", path="
              + premixAddress.toJSON().get("path")
              + " ("
              + premixUtxo.value
              + "sats)");
    } else {
      log.info(" • Connecting client " + (i + 1));
    }
    whirlpoolClient.whirlpool(mixParams, 1, listener);
    return whirlpoolClient;
  }
}
