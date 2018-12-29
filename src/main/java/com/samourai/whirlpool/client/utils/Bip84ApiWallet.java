package com.samourai.whirlpool.client.utils;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.MultiAddrResponse;
import com.samourai.api.beans.UnspentResponse.UnspentOutput;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.indexHandler.IIndexHandler;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bip84ApiWallet extends Bip84Wallet {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int INIT_BIP84_RETRY = 3;
  private static final int INIT_BIP84_RETRY_TIMEOUT = 5000;
  private SamouraiApi samouraiApi;

  public Bip84ApiWallet(
      HD_Wallet bip84w,
      int accountIndex,
      IIndexHandler indexHandler,
      SamouraiApi samouraiApi,
      boolean init)
      throws Exception {
    super(bip84w, accountIndex, indexHandler);
    this.samouraiApi = samouraiApi;

    if (init) {
      initBip84();
    }

    if (indexHandler.get() == 0) {
      // fetch index from API
      int nextIndex = fetchNextAddressIndex();
      if (log.isDebugEnabled()) {
        log.debug("Resuming index from API: " + nextIndex);
      }
      indexHandler.set(nextIndex);
    }
  }

  public List<UnspentOutput> fetchUtxos() throws Exception {
    String zpub = getZpub();
    return samouraiApi.fetchUtxos(zpub);
  }

  private int fetchNextAddressIndex() throws Exception {
    String zpub = getZpub();
    MultiAddrResponse.Address address = samouraiApi.fetchAddress(zpub);
    if (address == null) {
      throw new Exception("Address not found");
    }
    return address.account_index;
  }

  public void initBip84() throws Exception {
    for (int i = 0; i < INIT_BIP84_RETRY; i++) {
      log.info(" • Initializing bip84 wallet: " + accountIndex);
      try {
        samouraiApi.initBip84(getZpub());
        return; // success
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("", e);
        }
        log.error(
            " x Initializing bip84 wallet failed, retrying... ("
                + (i + 1)
                + "/"
                + INIT_BIP84_RETRY
                + ")");
        Thread.sleep(INIT_BIP84_RETRY_TIMEOUT);
      }
    }
    throw new NotifiableException("Unable to initialize Bip84 wallet");
  }
}
