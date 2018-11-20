package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class Bip84WalletTest extends AbstractTest {
  private static final String SEED_WORDS =
      "hub casual home drift winter such economy wage waste wagon essay torch";
  private static final String SEED_PASSPHRASE = "whirlpool";
  private Bip84Wallet bip84Wallet;

  @Override
  public void setup() throws Exception {
    super.setup();

    HD_Wallet bip84w = hdWalletFactory.restoreWallet(SEED_WORDS, SEED_PASSPHRASE, 1, params);
    bip84Wallet = new Bip84Wallet(bip84w, Integer.MAX_VALUE, null);
  }

  @Test
  public void getAddressAt() throws Exception {
    Assert.assertEquals(
        "tb1q5yg72kwrjrtuss5fkd767w838mpkfkn7gdndn8", toBech32(bip84Wallet.getAddressAt(0, 0)));
    Assert.assertEquals(
        "tb1qycxpd2939qgfye9vn5523aqcn7djd2np6w88cd", toBech32(bip84Wallet.getAddressAt(0, 15)));
    Assert.assertEquals(
        "tb1q0y5vnqt46lcqyk698785lc7rfscf9hpu5vcr0w", toBech32(bip84Wallet.getAddressAt(1, 0)));
    Assert.assertEquals(
        "tb1qycxpd2939qgfye9vn5523aqcn7djd2np6w88cd", toBech32(bip84Wallet.getAddressAt(0, 15)));
  }

  @Test
  public void getNextAddress() throws Exception {
    Assert.assertEquals(
        toBech32(bip84Wallet.getAddressAt(0, 0)), toBech32(bip84Wallet.getNextAddress()));
    Assert.assertEquals(
        toBech32(bip84Wallet.getAddressAt(0, 1)), toBech32(bip84Wallet.getNextAddress()));
    Assert.assertEquals(
        toBech32(bip84Wallet.getAddressAt(0, 2)), toBech32(bip84Wallet.getNextAddress()));
  }

  @Test
  public void getZpub() throws Exception {
    Assert.assertEquals(
        "vpub5ZM4gR44LSpwjm81YNCvwMtadNj9bABxqq4xRibndVUqbpis8TuzhXdWjVYZ6cxrorPngwJrSgwnjkJoUVBW9Ldnx63Rt4hSV2zjfQcZ4oX",
        bip84Wallet.getZpub());
  }

  private String toBech32(HD_Address hdAddress) {
    return bech32Util.toBech32(hdAddress, params);
  }
}
