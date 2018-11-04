package com.samourai.whirlpool.client.run;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.SegwitAddress;
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

  private Bip84Wallet postmixWallet;
  private HD_Address receiveAddress;

  public Bip84PostmixHandler(Bip84Wallet postmixWallet) {
    this.postmixWallet = postmixWallet;
    this.receiveAddress = null;
  }

  @Override
  public synchronized String computeReceiveAddress(NetworkParameters params) throws Exception {
    this.receiveAddress = postmixWallet.getNextAddress();

    String bech32Address =
        new SegwitAddress(receiveAddress.getPubKey(), params).getBech32AsString();
    log.info(
        "receiveAddress="
            + bech32Address
            + ", receiveKey="
            + receiveAddress.getECKey().getPrivateKeyAsWiF(params)
            + ", path="
            + receiveAddress.toJSON().get("path"));
    return bech32Address;
  }

  @Override
  public IPremixHandler computeNextPremixHandler(UtxoWithBalance receiveUtxo) {
    ECKey receiveKey = receiveAddress.getECKey();
    return new PremixHandler(receiveUtxo, receiveKey);
  }
}
