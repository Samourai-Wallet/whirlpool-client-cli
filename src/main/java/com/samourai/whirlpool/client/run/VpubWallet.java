package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.MultiAddrResponse;
import com.samourai.api.beans.UnspentResponse.UnspentOutput;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import java.util.List;

public class VpubWallet {
  private HD_Wallet bip44w;
  private BIP47Wallet bip47w;
  private HD_Wallet bip84w;
  private SamouraiApi samouraiApi;

  public VpubWallet(
      HD_Wallet bip44w, BIP47Wallet bip47w, HD_Wallet bip84w, SamouraiApi samouraiApi) {
    this.bip44w = bip44w;
    this.bip47w = bip47w;
    this.bip84w = bip84w;
    this.samouraiApi = samouraiApi;
  }

  public List<UnspentOutput> fetchUtxos(int accountIdx) throws Exception {
    String zpub = bip84w.getAccountAt(accountIdx).zpubstr();
    return samouraiApi.fetchUtxos(zpub);
  }

  public MultiAddrResponse.Address fetchAddress(int accountIdx) throws Exception {
    String zpub = bip84w.getAccountAt(accountIdx).zpubstr();
    MultiAddrResponse.Address address = samouraiApi.findAddress(zpub);
    if (address == null) {
      throw new Exception("Address not found");
    }
    return address;
  }

  public HD_Wallet getBip44w() {
    return bip44w;
  }

  public BIP47Wallet getBip47w() {
    return bip47w;
  }

  public HD_Wallet getBip84w() {
    return bip84w;
  }
}
