package com.samourai.whirlpool.client.run;

import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.WhirlpoolClient;
import com.samourai.whirlpool.client.run.vpub.*;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SamouraiApi {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String URL_BACKEND = "https://api.samouraiwallet.com/test";
    private static final String URL_UNSPENT = "/v2/unspent?active=";
    private static final String URL_MULTIADDR = "/v2/multiaddr?active=";
    private static final String URL_FEES = "/v2/fees";

    private IHttpClient httpClient;

    public SamouraiApi(IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public List<UnspentResponse.UnspentOutput> fetchUtxos(String vpub) throws Exception {
        String url = URL_BACKEND + URL_UNSPENT + vpub;
        UnspentResponse unspentResponse = httpClient.parseJson(url, UnspentResponse.class);
        List<UnspentResponse.UnspentOutput> unspentOutputs = new ArrayList<>();
        if (unspentResponse.unspent_outputs != null) {
            unspentOutputs = Arrays.asList(unspentResponse.unspent_outputs);
        }
        return unspentOutputs;
    }

    public List<MultiAddrResponse.Address> fetchAddresses(String vpub) throws Exception {
        String url = URL_BACKEND + URL_MULTIADDR + vpub;
        MultiAddrResponse multiAddrResponse = httpClient.parseJson(url, MultiAddrResponse.class);
        List<MultiAddrResponse.Address> addresses = new ArrayList<>();
        if (multiAddrResponse.addresses != null) {
            addresses = Arrays.asList(multiAddrResponse.addresses);
        }
        return addresses;
    }

    public MultiAddrResponse.Address findAddress(String vpub) throws Exception {
        List<MultiAddrResponse.Address> addresses = fetchAddresses(vpub);
        if (addresses.size() != 1) { // TODO find addres by ????
            throw new Exception("Address count=" + addresses.size());
        }
        return addresses.get(0);
    }

    public int fetchFees() throws Exception {
        String url = URL_BACKEND + URL_FEES;
        Map feesResponse = httpClient.parseJson(url, Map.class);
        return Integer.parseInt(feesResponse.get("2").toString());
    }
}
