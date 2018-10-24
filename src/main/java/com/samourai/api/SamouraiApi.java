package com.samourai.api;

import com.samourai.api.beans.MultiAddrResponse;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.http.client.IHttpClient;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamouraiApi {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String URL_BACKEND = "https://api.samouraiwallet.com/test";
  private static final String URL_UNSPENT = "/v2/unspent?active=";
  private static final String URL_MULTIADDR = "/v2/multiaddr?active=";
  private static final String URL_FEES = "/v2/fees";
  private static final int MAX_FEE_PER_BYTE = 500;

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
    int fees2 = Integer.parseInt(feesResponse.get("2").toString());
    if (fees2 < 1) {
      log.error("Invalid fee response from server: fees2=" + fees2);
      throw new Exception("Invalid fee response from server");
    }
    return Math.min(fees2, MAX_FEE_PER_BYTE);
  }
}
