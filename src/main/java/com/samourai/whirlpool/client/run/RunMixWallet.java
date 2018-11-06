package com.samourai.whirlpool.client.run;

import com.samourai.api.beans.UnspentResponse;
import com.samourai.stomp.client.JavaStompClient;
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
import org.bitcoinj.core.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMixWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private WhirlpoolClientConfig config;
  private Bip84Wallet depositAndPremixWallet;
  private Bip84Wallet postmixWallet;
  private int clientDelay;
  private int nbClients;
  private Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  public RunMixWallet(
      WhirlpoolClientConfig config,
      Bip84Wallet depositAndPremixWallet,
      Bip84Wallet postmixWallet,
      int clientDelay,
      int nbClients) {
    this.config = config;
    this.depositAndPremixWallet = depositAndPremixWallet;
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
      UtxoWithBalance premixUtxoWithBalance =
          new UtxoWithBalance(premixUtxo.tx_hash, premixUtxo.tx_output_n, premixUtxo.value);

      // input key from premix
      HD_Address premixAddress = depositAndPremixWallet.getAddressAt(premixUtxo);
      String premixAddressBech32 =
          bech32Util.toBech32(premixAddress, config.getNetworkParameters());
      ECKey premixKey = premixAddress.getECKey();
      IPremixHandler premixHandler = new PremixHandler(premixUtxoWithBalance, premixKey);

      // one config / StompClient per client
      WhirlpoolClientConfig clientConfig = new WhirlpoolClientConfig(config);
      clientConfig.setStompClient(new JavaStompClient());
      WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(clientConfig);

      log.info(
          " • Connecting client #"
              + (i + 1)
              + ": mustMix, premixUtxo="
              + premixUtxo
              + ", premixKey="
              + premixKey.getPrivateKeyAsWiF(config.getNetworkParameters())
              + ", premixAddress="
              + premixAddressBech32
              + ", path="
              + premixAddress.toJSON().get("path")
              + " ("
              + premixUtxo.value
              + "sats)");
      IPostmixHandler postmixHandler = new Bip84PostmixHandler(postmixWallet);
      MixParams mixParams =
          new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);
      WhirlpoolClientListener listener = multiClientManager.register(whirlpoolClient);
      whirlpoolClient.whirlpool(mixParams, 1, listener);

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
}
