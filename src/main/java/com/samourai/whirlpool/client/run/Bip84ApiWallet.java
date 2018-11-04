package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.MultiAddrResponse;
import com.samourai.api.beans.UnspentResponse.UnspentOutput;
import com.samourai.wallet.hd.HD_Wallet;
import java.util.List;

public class Bip84ApiWallet extends Bip84Wallet {
  private SamouraiApi samouraiApi;

  public Bip84ApiWallet(HD_Wallet bip84w, int accountIndex, SamouraiApi samouraiApi)
      throws Exception {
    super(bip84w, accountIndex, 0);
    this.samouraiApi = samouraiApi;
    this.nextAddressIndex = fetchNextAddressIndex();
  }

  public List<UnspentOutput> fetchUtxos() throws Exception {
    String zpub = bip84w.getAccountAt(accountIndex).zpubstr();
    return samouraiApi.fetchUtxos(zpub);
  }

  private int fetchNextAddressIndex() throws Exception {
    String zpub = bip84w.getAccountAt(accountIndex).zpubstr();
    MultiAddrResponse.Address address = samouraiApi.findAddress(zpub);
    if (address == null) {
      throw new Exception("Address not found");
    }
    return address.account_index;
  }
}
