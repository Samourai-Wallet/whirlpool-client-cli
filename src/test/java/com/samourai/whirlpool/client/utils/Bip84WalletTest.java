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

    HD_Wallet bip84w = hdWalletFactory.restoreWallet(SEED_WORDS, SEED_PASSPHRASE, 1, params);
    bip84Wallet = new Bip84Wallet(bip84w, Integer.MAX_VALUE, new MemoryIndexHandler());
  }

  @Test
  public void getAddressAt() throws Exception {
    Assert.assertEquals(
        "tb1qz4lz7f82uesq3nzl29dlrxyuuksjwxp7sufrv8", toBech32(bip84Wallet.getAddressAt(0, 0)));
    Assert.assertEquals(
        "tb1qpaamtpfe3glzl5lzuals058tf8x2jmqtgc29th", toBech32(bip84Wallet.getAddressAt(0, 15)));
    Assert.assertEquals(
        "tb1qy6zc2fusaxy7n2z4pdkw8s7ycyz3jvwaauuh5a", toBech32(bip84Wallet.getAddressAt(1, 0)));
    Assert.assertEquals(
        "tb1qpaamtpfe3glzl5lzuals058tf8x2jmqtgc29th", toBech32(bip84Wallet.getAddressAt(0, 15)));
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
        "vpub5Y7ApGPEn58hVWY7rQLJ3465UNC4QPacc4iB5KJZCw8NCu365xLQYmP31AFpiwNFJzJt2Bu3Gwc8okofkTqkY6waeGNrsWC9PPbWUw5d6XE",
        bip84Wallet.getZpub());
  }

  private String toBech32(HD_Address hdAddress) {
    return bech32Util.toBech32(hdAddress, params);
  }
}
