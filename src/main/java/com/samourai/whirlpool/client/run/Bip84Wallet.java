package com.samourai.whirlpool.client.run;

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

  public HD_Address getNextAddress() throws Exception {
    int nextAddressIndex = getNextAddressIndex();
    return getAddressAt(nextAddressIndex);
  }

  public HD_Address getAddressAt(int addressIndex) throws Exception {
    return getAddressBip84(accountIndex, CHAIN, addressIndex);
  }

  private int getNextAddressIndex() throws Exception {
    // increment on each call
    nextAddressIndex++;
    return nextAddressIndex - 1;
  }

  private HD_Address getAddressBip84(int account, int chain, int index) {
    return bip84w.getAccountAt(account).getChain(chain).getAddressAt(index);
  }
}
