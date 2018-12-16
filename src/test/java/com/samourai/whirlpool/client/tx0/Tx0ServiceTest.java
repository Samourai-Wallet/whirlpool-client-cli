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
    HD_Wallet bip84w = hdWalletFactory.restoreWallet(seedWords, passphrase, 1, params);
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
        "cde7c4c33428a584e371fdbd963a3905a55d3b49f76780e4a44c1085b6a06b91", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff080000000000000000426a401a2ceb49ee8268488cd85c69be419eb8968b357f450431e236dbadb35d9de16daf80676573d1e00a87bfeb5223c36351398940ff44f0b69dad62b060b82b537de803000000000000160014c25132cfe8371e4c4ab5d046db21e18e804a7625d6420f00000000001600141384e5f8b6ec64d7276b1f6f5d320af0def4a014d6420f00000000001600146f37da8f91ac1131bba65f7b7cdbbfbec4ced57cd6420f00000000001600147b80c569987972ec2753a5708b61fbaef60ad6d0d6420f000000000016001482545e4232941f47e7abcd8042e6514fbc968305d6420f0000000000160014b7017582609a12233e4019dac754d660db6963668839801d00000000160014f9d3a39cb5f697ab3d85dfa4cc1b848520cc241002483045022100bec2b0a7ab85acd1d5a5e36562f23d2c4570f73a08e7f16ed4f0dc627b9b8ea602202ae8083dfca5f2834a3b709f42d517b8c1df44ceee662a342b0f7d126a1f901a012103ae9a9884b779bb72ba1d6d0da99c1f0f361f8ff9ab7ef4a8062e00014689359100000000",
        tx0Hex);
  }
}
