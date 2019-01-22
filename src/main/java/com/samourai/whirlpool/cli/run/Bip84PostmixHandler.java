package com.samourai.whirlpool.cli.run;

import com.samourai.wallet.client.Bip84Wallet;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.client.mix.handler.IPostmixHandler;
import com.samourai.whirlpool.client.mix.handler.IPremixHandler;
import com.samourai.whirlpool.client.mix.handler.PremixHandler;
import com.samourai.whirlpool.client.mix.handler.UtxoWithBalance;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84PostmixHandler implements IPostmixHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  private Bip84Wallet postmixWallet;
  private HD_Address receiveAddress;

  public Bip84PostmixHandler(Bip84Wallet postmixWallet) {
    this.postmixWallet = postmixWallet;
    this.receiveAddress = null;
  }

  @Override
  public synchronized String computeReceiveAddress(NetworkParameters params) throws Exception {
    this.receiveAddress = postmixWallet.getNextAddress();

    String bech32Address = bech32Util.toBech32(receiveAddress, params);
    if (log.isDebugEnabled()) {
      log.debug(
          "receiveAddress=" + bech32Address + ", path=" + receiveAddress.toJSON().get("path"));
    }
    return bech32Address;
  }

  @Override
  public IPremixHandler computeNextPremixHandler(UtxoWithBalance receiveUtxo) {
    ECKey receiveKey = receiveAddress.getECKey();
    return new PremixHandler(receiveUtxo, receiveKey);
  }
}
