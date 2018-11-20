package com.samourai.whirlpool.client.test;

import com.samourai.wallet.hd.HD_WalletFactoryJava;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Before;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class AbstractTest {
  protected NetworkParameters params = TestNet3Params.get();
  protected HD_WalletFactoryJava hdWalletFactory = HD_WalletFactoryJava.getInstance();
  protected Bech32UtilGeneric bech32Util = Bech32UtilGeneric.getInstance();

  @Before
  public void setup() throws Exception {}

  @After
  public void tearDown() throws Exception {}
}
