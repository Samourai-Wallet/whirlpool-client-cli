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
  private static final int FAILOVER_FEE_PER_BYTE = 400;
  public static final int SLEEP_REFRESH_UTXOS = 10000;

  private IHttpClient httpClient;

  public SamouraiApi(IHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public List<UnspentResponse.UnspentOutput> fetchUtxos(String zpub) throws Exception {
    String url = URL_BACKEND + URL_UNSPENT + zpub;
    if (log.isDebugEnabled()) {
      log.debug("fetchUtxos: " + url);
    }
    UnspentResponse unspentResponse = httpClient.parseJson(url, UnspentResponse.class);
    List<UnspentResponse.UnspentOutput> unspentOutputs = new ArrayList<>();
    if (unspentResponse.unspent_outputs != null) {
      unspentOutputs = Arrays.asList(unspentResponse.unspent_outputs);
    }
    return unspentOutputs;
  }

  public List<MultiAddrResponse.Address> fetchAddresses(String zpub) throws Exception {
    String url = URL_BACKEND + URL_MULTIADDR + zpub;
    if (log.isDebugEnabled()) {
      log.debug("fetchAddress: " + url);
    }
    MultiAddrResponse multiAddrResponse = httpClient.parseJson(url, MultiAddrResponse.class);
    List<MultiAddrResponse.Address> addresses = new ArrayList<>();
    if (multiAddrResponse.addresses != null) {
      addresses = Arrays.asList(multiAddrResponse.addresses);
    }
    return addresses;
  }

  public MultiAddrResponse.Address fetchAddress(String zpub) throws Exception {
    List<MultiAddrResponse.Address> addresses = fetchAddresses(zpub);
    if (addresses.size() != 1) {
      throw new Exception("Address count=" + addresses.size());
    }
    MultiAddrResponse.Address address = addresses.get(0);

    if (log.isDebugEnabled()) {
      log.debug(
          "fetchAddress "
              + zpub
              + ": account_index="
              + address.account_index
              + ", change_index="
              + address.change_index);
    }
    return address;
  }

  public int fetchFees() {
    return fetchFees(true);
  }

  private int fetchFees(boolean retry) {
    String url = URL_BACKEND + URL_FEES;
    int fees2 = 0;
    try {
      Map feesResponse = httpClient.parseJson(url, Map.class);
      fees2 = Integer.parseInt(feesResponse.get("2").toString());
    } catch (Exception e) {
      log.error("Invalid fee response from server", e);
    }
    if (fees2 < 1) {
      if (retry) {
        return fetchFees(false);
      }
      return FAILOVER_FEE_PER_BYTE;
    }
    return Math.min(fees2, MAX_FEE_PER_BYTE);
  }
}
