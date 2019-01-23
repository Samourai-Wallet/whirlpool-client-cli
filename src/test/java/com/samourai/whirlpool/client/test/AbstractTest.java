package com.samourai.whirlpool.client.test;

import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import org.bitcoinj.core.NetworkParameters;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class AbstractTest {
  @Autowired protected NetworkParameters params;
  @Autowired protected HD_WalletFactoryJava hdWalletFactory;
  @Autowired protected Bech32UtilGeneric bech32Util;

  @Before
  public void setup() throws Exception {}

  @After
  public void tearDown() throws Exception {}
}
