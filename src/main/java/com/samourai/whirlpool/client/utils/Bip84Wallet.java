package com.samourai.whirlpool.client.utils;

import com.samourai.api.beans.UnspentResponse;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84Wallet {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected static final int CHAIN = 0;

  protected HD_Wallet bip84w;
  protected int accountIndex;
  protected int nextAddressIndex;

  public Bip84Wallet(HD_Wallet bip84w, int accountIndex, int nextAddressIndex) {
    this.bip84w = bip84w;
    this.accountIndex = accountIndex;
    this.nextAddressIndex = nextAddressIndex;
  }

  public HD_Address getNextAddress() {
    int nextAddressIndex = getNextAddressIndex();
    return getAddressAt(CHAIN, nextAddressIndex);
  }

  public HD_Address getAddressAt(int chainIndex, int addressIndex) {
    return getAddressBip84(accountIndex, chainIndex, addressIndex);
  }

  public HD_Address getAddressAt(UnspentResponse.UnspentOutput utxo) {
    return getAddressAt(utxo.computePathChainIndex(), utxo.computePathAddressIndex());
  }

  public String getZpub() {
    String zpub = bip84w.getAccountAt(accountIndex).zpubstr();
    if (log.isDebugEnabled()) {
      log.debug("zpub for account #" + accountIndex + ": " + zpub);
    }
    return zpub;
  }

  private int getNextAddressIndex() {
    // increment on each call
    nextAddressIndex++;
    return nextAddressIndex - 1;
  }

  private HD_Address getAddressBip84(int account, int chain, int index) {
    return bip84w.getAccountAt(account).getChain(chain).getAddressAt(index);
  }

  public void setNextAddressIndex(int nextAddressIndex) {
    this.nextAddressIndex = nextAddressIndex;
  }
}
