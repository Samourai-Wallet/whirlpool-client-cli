package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.utils.Bip84Wallet;
import java.lang.invoke.MethodHandles;
import org.bitcoinj.core.Coin;
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
    String seedWords =
        "recipe obtain chunk amused split second disorder budget okay verb border rifle";
    String passphrase = "whirlpool";
    HD_Wallet bip84w = CliUtils.computeBip84Wallet(passphrase, seedWords, params, hdWalletFactory);
    HD_Address spendFromAddress = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
    TransactionOutPoint spendFromOutpoint =
        new TransactionOutPoint(
            params,
            1,
            Sha256Hash.wrap("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae"),
            Coin.valueOf(500000000));
    Bip84Wallet depositAndPremixWallet = new Bip84Wallet(bip84w, 0, 62);
    int nbOutputs = 5;
    long destinationValue = 1000150;
    long tx0MinerFee = 150;
    String xpubSamouraiFees =
        "vpub5YS8pQgZKVbrSn9wtrmydDWmWMjHrxL2mBCZ81BDp7Z2QyCgTLZCrnBprufuoUJaQu1ZeiRvUkvdQTNqV6hS96WbbVZgweFxYR1RXYkBcKt";
    long samouraiFees = 1000;

    Tx0 tx0 =
        tx0Service.tx0(
            spendFromAddress,
            spendFromOutpoint,
            nbOutputs,
            depositAndPremixWallet,
            destinationValue,
            tx0MinerFee,
            xpubSamouraiFees,
            samouraiFees);
    String tx0Hash = tx0.getTx().getHashAsString();
    String tx0Hex = new String(Hex.encode(tx0.getTx().bitcoinSerialize()));
    Assert.assertEquals(
        "40bed460423e461f98f00dba5f9b07ba6f5af7daca5cf5ecd16cb466457041f9", tx0Hash);
    Assert.assertEquals(
        "01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff080000000000000000066a0400000000e803000000000000160014c25132cfe8371e4c4ab5d046db21e18e804a7625d6420f0000000000160014150e6675de4649d1b798c6f82d7213203a1dd053d6420f0000000000160014260663c929e115f7b5e870ed69876950c5f392f9d6420f00000000001600145b82acbd922c8d795c5fcaa07e648f4e6ad1f3c5d6420f00000000001600145e5e83bbdd188cfa1373b8f85c23b5197eaa4ec7d6420f0000000000160014914cd104d9524b55226fa84e2f1d714b74705b0ea6d07f1d000000001600140816ab9d6d6da88d16629ba9927ae22da22bb7fa024830450221009f2aba47cd4c14a57057d23e1dee59af557f4d4f88581437079c840ab8a533fd022065f1349f8b92489702e34ed111ada53bb4200f11cb651edd93d753d68c9052d7012102cd718166e324dde323106acb320a4118786ba70b6b9db49a703f0e6d3df6218400000000",
        tx0Hex);
  }
}
