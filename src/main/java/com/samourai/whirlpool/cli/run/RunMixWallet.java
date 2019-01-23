package com.samourai.whirlpool.cli.run;

import com.samourai.api.client.beans.UnspentResponse;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.Bip84PostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
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
  private CliTorClientService torClientService;
  private CliWalletService cliWalletService;
  private Bech32UtilGeneric bech32Util;

  public RunMixWallet(
      WhirlpoolClientConfig config,
      CliTorClientService torClientService,
      CliWalletService cliWalletService,
      Bech32UtilGeneric bech32Util) {
    this.config = config;
    this.torClientService = torClientService;
    this.cliWalletService = cliWalletService;
    this.bech32Util = bech32Util;
  }

  public boolean runMix(
      List<UnspentResponse.UnspentOutput> mustMixUtxosPremix,
      Pool pool,
      int nbClients,
      int clientDelay)
      throws Exception {
    MultiClientManager multiClientManager = new MultiClientManager();

    // connect each client
    for (int i = 0; i < nbClients; i++) {
      // pick last mustMix
      UnspentResponse.UnspentOutput premixUtxo =
          mustMixUtxosPremix.remove(mustMixUtxosPremix.size() - 1);

      // one config / StompClient per client
      WhirlpoolClientConfig clientConfig = new WhirlpoolClientConfig(config);
      clientConfig.setStompClient(new JavaStompClient(torClientService));
      WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(clientConfig);
      WhirlpoolClientListener listener = multiClientManager.register(whirlpoolClient);

      // start in a new thread as we may wait for TOR connexion
      final int iClient = i;
      new Thread(
              () -> {
                if (torClientService.getTorClient().isPresent()) {
                  // for N clients we need N TOR connexions ready to register outputs
                  torClientService.getTorClient().get().waitConnexionReady(iClient + 1);
                }
                runMixClient(whirlpoolClient, listener, premixUtxo, iClient, pool);
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

  private void runMixClient(
      WhirlpoolClient whirlpoolClient,
      WhirlpoolClientListener listener,
      UnspentResponse.UnspentOutput premixUtxo,
      int i,
      Pool pool) {
    UtxoWithBalance premixUtxoWithBalance =
        new UtxoWithBalance(premixUtxo.tx_hash, premixUtxo.tx_output_n, premixUtxo.value);

    try {
      // input key from premix
      CliWallet cliWallet = cliWalletService.getCliWallet();
      HD_Address premixAddress = cliWallet.getPremixWallet().getAddressAt(premixUtxo);
      String premixAddressBech32 =
          bech32Util.toBech32(premixAddress, config.getNetworkParameters());
      ECKey premixKey = premixAddress.getECKey();
      IPremixHandler premixHandler = new PremixHandler(premixUtxoWithBalance, premixKey);
      IPostmixHandler postmixHandler = new Bip84PostmixHandler(cliWallet.getPostmixWallet());
      MixParams mixParams =
          new MixParams(pool.getPoolId(), pool.getDenomination(), premixHandler, postmixHandler);

      if (log.isDebugEnabled()) {
        log.debug(
            " • Connecting client #"
                + (i + 1)
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
    } catch (Exception e) {
      log.error(" • ERROR connecting client " + (i + 1) + ": " + e.getMessage(), e);
    }
  }
}
