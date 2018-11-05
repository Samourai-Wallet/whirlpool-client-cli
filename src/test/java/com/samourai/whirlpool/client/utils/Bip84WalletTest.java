package com.samourai.whirlpool.client.utils;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.test.AbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class Bip84WalletTest extends AbstractTest {
  private static final String SEED_WORDS =
      "recipe obtain chunk amused split second disorder budget okay verb border rifle";
  private static final String SEED_PASSPHRASE = "whirlpool";

  private Bip84Wallet bip84Wallet;

  @Override
  public void setup() throws Exception {
    super.setup();

    HD_Wallet bip84w =
        CliUtils.computeBip84Wallet(SEED_PASSPHRASE, SEED_WORDS, params, hdWalletFactory);
    bip84Wallet = new Bip84Wallet(bip84w, Integer.MAX_VALUE, 0);
  }

  @Test
  public void getAddressAt() throws Exception {
    Assert.assertEquals(
        "tb1qk8a6nwyvps5grufdzs8qsq74t2ujnaj3lxfyh0", toBech32(bip84Wallet.getAddressAt(0, 0)));
    Assert.assertEquals(
        "tb1qmu2ndz9r49ce4w3jfptd8n7pugf9vg2p6pwuxp", toBech32(bip84Wallet.getAddressAt(0, 15)));
    Assert.assertEquals(
        "tb1qzmkkt8xvtf8s8t8wum95wa6d54gg3z3xzmcl9k", toBech32(bip84Wallet.getAddressAt(1, 0)));
    Assert.assertEquals(
        "tb1qmu2ndz9r49ce4w3jfptd8n7pugf9vg2p6pwuxp", toBech32(bip84Wallet.getAddressAt(0, 15)));
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
        "vpub5Yw9AZRQ4ekAvoZgngxgpoqHzMjJzza1XENBPMjViXTGjDFeXznusXGruHnvyqVfej7ZBiXSqr1XDSXmtDZgAV1eE1kbKEJbC2KRveKJihr",
        bip84Wallet.getZpub());
  }

  private String toBech32(HD_Address hdAddress) {
    return bech32Util.toBech32(hdAddress, params);
  }
}
