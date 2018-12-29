package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.utils.indexHandler.MemoryIndexHandler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class Bip84WalletTest extends AbstractTest {
  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE = "whirlpool";
  private Bip84Wallet bip84Wallet;

  @Override
  public void setup() throws Exception {
    super.setup();

    byte[] seed = hdWalletFactory.computeSeedFromWords(SEED_WORDS);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, SEED_PASSPHRASE, params);
    bip84Wallet = new Bip84Wallet(bip84w, Integer.MAX_VALUE, new MemoryIndexHandler());
  }

  @Test
  public void getAddressAt() throws Exception {
    Assert.assertEquals(
        "tb1q5lc455emwwttdqwf9p32xf8fhgrhvfp5vxvul7", toBech32(bip84Wallet.getAddressAt(0, 0)));
    Assert.assertEquals(
        "tb1q2vw863w92dwpej48maqyjazj4ch3x0krzrw9cs", toBech32(bip84Wallet.getAddressAt(0, 15)));
    Assert.assertEquals(
        "tb1qtfrd7zug2qkhv3nc6294pls92qru6vvqse40dw", toBech32(bip84Wallet.getAddressAt(1, 0)));
    Assert.assertEquals(
        "tb1q2vw863w92dwpej48maqyjazj4ch3x0krzrw9cs", toBech32(bip84Wallet.getAddressAt(0, 15)));
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
        "vpub5YEQpEDXWE3TcFX9JXj73TaBskrDTy5pdw3HNujngNKfAYtgx1ynNd6ri92A8Jdgccm9BX4S8yo45hsK4oiCar15pqA7MHM9XtkzNySdknj",
        bip84Wallet.getZpub());
  }

  private String toBech32(HD_Address hdAddress) {
    return bech32Util.toBech32(hdAddress, params);
  }
}
