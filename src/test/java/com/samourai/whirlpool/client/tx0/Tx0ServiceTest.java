package com.samourai.whirlpool.client.tx0;

import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.hd.HD_Chain;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.run.vpub.HdWalletFactory;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;

@RunWith(SpringJUnit4ClassRunner.class)
public class Tx0ServiceTest {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private NetworkParameters params = TestNet3Params.get();
    private Tx0Service tx0Service = new Tx0Service(params);
    private MnemonicCode mc = CliUtils.computeMnemonicCode();
    private HdWalletFactory hdWalletFactory = new HdWalletFactory(params, mc);

    @Test
    public void tx0() throws Exception {
        String seedWords = "recipe obtain chunk amused split second disorder budget okay verb border rifle";
        String passphrase = "whirlpool";
        HD_Wallet bip44w = hdWalletFactory.restoreWallet(seedWords, passphrase, 1);
        HD_Wallet bip84w = new HD_Wallet(84, mc, params, Hex.decode(bip44w.getSeedHex()), bip44w.getPassphrase(), 1);
        HD_Address spendFromAddress = bip84w.getAccountAt(0).getChain(0).getAddressAt(61);
        TransactionOutPoint spendFromOutpoint = new TransactionOutPoint(params, 1, Sha256Hash.wrap("cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae"), Coin.valueOf(500000000));
        int nbOutputs = 5;
        HD_Chain destinationChain = bip84w.getAccountAt(Integer.MAX_VALUE - 1).getChain(0);
        long destinationValue = 1000150;
        int destinationIndex = 62;
        HD_Address changeAddress = bip84w.getAccount(0).getChain(0).getAddressAt(0);
        long tx0MinerFee = 150;
        String xpubSamouraiFees = "vpub5YS8pQgZKVbrSn9wtrmydDWmWMjHrxL2mBCZ81BDp7Z2QyCgTLZCrnBprufuoUJaQu1ZeiRvUkvdQTNqV6hS96WbbVZgweFxYR1RXYkBcKt";
        long samouraiFees = 1000;

        Tx0 tx0 = tx0Service.tx0(spendFromAddress, spendFromOutpoint,
                nbOutputs, destinationChain, destinationValue, destinationIndex,
                changeAddress, tx0MinerFee, xpubSamouraiFees, samouraiFees);
        String tx0Hash = tx0.getTx().getHashAsString();
        String tx0Hex = new String(Hex.encode(tx0.getTx().bitcoinSerialize()));
        Assert.assertEquals("efc421109a9d59d28aa423ecea4cd0601a4ac30a17a51f72afb81d5d8c053cbe", tx0Hash);
        Assert.assertEquals("01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff080000000000000000066a04000000006400000000000000160014c25132cfe8371e4c4ab5d046db21e18e804a7625d6420f000000000016001414913fdd23b6d405d484e836bed0750c2920f02fd6420f000000000016001453b4b14898200e879b7d74604c2a7c575eab43c1d6420f0000000000160014d278b761c1b31c42b8eb2cc239fbdf23452a3683d6420f0000000000160014d2a6f70df7b8398e9a998688e4bef932ae12471bd6420f0000000000160014fc92873d060e8aa08a066800877c0abbe05469a9d815811d00000000160014feff2bf49b638de0562c3dc132bef9038563450402483045022100b26d39c25f0f28084c867e8795d2a55a9bbaf91aef2a067bef479e27344027bb0220072af84cf29411d8f22a73880611ed4ebfbf594c40dc26c021df421899e1a655012102cd718166e324dde323106acb320a4118786ba70b6b9db49a703f0e6d3df6218400000000", tx0Hex);
    }

}