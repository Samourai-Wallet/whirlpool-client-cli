package com.samourai.whirlpool.client.run;

import com.samourai.api.SamouraiApi;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.api.beans.MultiAddrResponse;
import com.samourai.api.beans.UnspentResponse;

import java.util.List;

public class VpubWallet {
    private HD_Wallet bip44w;
    private BIP47Wallet bip47w;
    private HD_Wallet bip84w;
    private String vpub;

    public VpubWallet(HD_Wallet bip44w, BIP47Wallet bip47w, HD_Wallet bip84w, String vpub) {
        this.bip44w = bip44w;
        this.bip47w = bip47w;
        this.bip84w = bip84w;
        this.vpub = vpub;
    }

    public List<UnspentResponse.UnspentOutput> fetchUtxos(SamouraiApi samouraiApi) throws Exception {
        return samouraiApi.fetchUtxos(vpub);
    }

    public MultiAddrResponse.Address fetchAddress(SamouraiApi samouraiApi) throws Exception {
        MultiAddrResponse.Address address = samouraiApi.findAddress(vpub);
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

    public String getVpub() {
        return vpub;
    }
}
