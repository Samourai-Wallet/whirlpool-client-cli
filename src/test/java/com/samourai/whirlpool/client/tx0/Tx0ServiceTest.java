package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.utils.Bip84Wallet;
import com.samourai.whirlpool.client.utils.indexHandler.MemoryIndexHandler;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class Tx0ServiceTest extends AbstractTest {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Tx0Service tx0Service = new Tx0Service(params);

  @Test
  public void tx0() throws Exception {
    String seedWords = "all all all all all all all all all all all all";
    String passphrase = "whirlpool";
    byte[] seed = hdWalletFactory.computeSeedFromWords(seedWords);
    HD_Wallet bip84w = hdWalletFactory.getBIP84(seed, passphrase, params);

    ECKey spendFromKey = bip84w.getAccountAt(0).getChain(0).getAddressAt(61).getECKey();
    TransactionOutPoint spendFromOutpoint =
        new TransactionOutPoint(
            params,
            1,
            Sha256Hash.wrap("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae"),
            Coin.valueOf(500000000));
    Bip84Wallet depositAndPremixWallet = new Bip84Wallet(bip84w, 0, new MemoryIndexHandler());
    int nbOutputs = 5;
    long destinationValue = 1000150;
    long tx0MinerFee = 150;
    String feeXpub =
        "vpub5YS8pQgZKVbrSn9wtrmydDWmWMjHrxL2mBCZ81BDp7Z2QyCgTLZCrnBprufuoUJaQu1ZeiRvUkvdQTNqV6hS96WbbVZgweFxYR1RXYkBcKt";
    long feeValue = 1000;
    int feeIndice = 0;
    byte[] feePayload = new byte[] {1, 2};

    Tx0 tx0 =
        tx0Service.tx0(
            spendFromKey.getPrivKeyBytes(),
            spendFromOutpoint,
            nbOutputs,
            depositAndPremixWallet,
            destinationValue,
            tx0MinerFee,
            feeXpub,
            feeValue,
            "PM8TJXp19gCE6hQzqRi719FGJzF6AreRwvoQKLRnQ7dpgaakakFns22jHUqhtPQWmfevPQRCyfFbdDrKvrfw9oZv5PjaCerQMa3BKkPyUf9yN1CDR3w6",
            feeIndice,
            feePayload);
    String tx0Hash = tx0.getTx().getHashAsString();
    String tx0Hex = new String(Hex.encode(tx0.getTx().bitcoinSerialize()));
    log.info(tx0.getTx().toString());
    Assert.assertEquals(
        "6b18f695098245340b7fb37fe6f213f2194e14866cf2c7f5695e0832059c8b24", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff080000000000000000426a409ae6649a7b1fc8a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f685917e803000000000000160014ab270a0394bcf76dddb6a4e51a951533b6356632d6420f00000000001600140d64011caf447917ef39b4347ee4044f96dee498d6420f0000000000160014481b4a773ce5e5a97d4c7b358b71adf3b386b350d6420f0000000000160014ae076a245f1f81813e70e4f5eff2c53ab73ab97fd6420f0000000000160014b1c3dbae1e74ba0218c03c987e384da70ba0896ed6420f0000000000160014c37b09ba860c1483da42a918a6f7b885700dfa748839801d0000000016001428258d005f72aeb114d1bef9457a4ffeef8198dc0247304402206a7ba0efcba2fa710918554777c22cac57f4769778a83f1de7a0cc58637c3a5902207e41f7f67a6074bde1902e006da471fa27c650ddea8933b2823612645946724201210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }
}
