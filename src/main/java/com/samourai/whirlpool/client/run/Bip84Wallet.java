package com.samourai.whirlpool.client.run;

import com.samourai.api.beans.UnspentResponse;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;

public class Bip84Wallet {
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

  private int getNextAddressIndex() {
    // increment on each call
    nextAddressIndex++;
    return nextAddressIndex - 1;
  }

  private HD_Address getAddressBip84(int account, int chain, int index) {
    return bip84w.getAccountAt(account).getChain(chain).getAddressAt(index);
  }
}
