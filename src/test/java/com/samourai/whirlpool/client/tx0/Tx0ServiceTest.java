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
    Bip84Wallet depositWallet = new Bip84Wallet(bip84w, 0, new MemoryIndexHandler());
    Bip84Wallet premixWallet =
        new Bip84Wallet(bip84w, Integer.MAX_VALUE - 2, new MemoryIndexHandler());
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
            depositWallet,
            premixWallet,
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
        "44323b67a641d930886c1a680dbde8e1239263b20f33ff642e0d1179092b6c6b", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff080000000000000000426a409ae6649a7b1fc8a917f408cbf7b41e27f3a5484650aafdf5167852bd348afa8aa8213dda856188683ab187a902923e7ec3b672a6fbb637a4063c71879f685917e803000000000000160014ae076a245f1f81813e70e4f5eff2c53ab73ab97fd6420f00000000001600141dffe6e395c95927e4a16e8e6bd6d05604447e4dd6420f000000000016001472df8c59071778ec20264e2aeb54dd4024bcee0ad6420f00000000001600147aca3eeaecc2ffefd434c70ed67bd579e629c29dd6420f00000000001600149676ec398c2fe0736d61e09e1136958b4bf40cdad6420f0000000000160014ff715cbded0e6205a68a1f66a52ee56d56b44c818839801d000000001600140d64011caf447917ef39b4347ee4044f96dee49802483045022100e4145e5ee1f37eb468283f570547e6dc1f6073fc16c0e7d531d0f9c52cb30206022041271195a0aded8155afe8f025d57a10d1031b50cde58b3345df4add9c01b0fa01210349baf197181fe53937d225d0e7bd14d8b5f921813c038a95d7c2648500c119b000000000",
        tx0Hex);
  }
}
