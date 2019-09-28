package com.samourai.whirlpool.cli.beans;

import com.samourai.wallet.api.pairing.PairingNetwork;
import com.samourai.wallet.api.pairing.PairingVersion;
import com.samourai.whirlpool.client.exception.NotifiableException;
import org.junit.Assert;
import org.junit.Test;

public class WhirlpoolPairingPayloadTest {

  @Test
  public void parse_valid() throws Exception {
    String payload;

    // valid
    payload =
        "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"0qTfDpexBYZ7GM0/F1xCnXctAKLPNcd8+U+GYWNDq7jHxGtsbcfwSeHI0BoVMSm7KrdIgBiKhyUl0XCntfq9drU6nOrtmqo2x1dppnvrLjNI71go2ICospLOtRHiFUac\"}}";
    parse(
        payload,
        PairingVersion.V1_0_0,
        PairingNetwork.TESTNET,
        "0qTfDpexBYZ7GM0/F1xCnXctAKLPNcd8+U+GYWNDq7jHxGtsbcfwSeHI0BoVMSm7KrdIgBiKhyUl0XCntfq9drU6nOrtmqo2x1dppnvrLjNI71go2ICospLOtRHiFUac",
        true); // passphrase=true for V1

    // valid
    payload =
        "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"BcXzXesuQjLKTgS54rzPBdyJ43IgWdvEXwNJo/ZE49hq8U5eLp/ge+4XQibNAeJS+Eng7AY19hiAIoR3vsTdsyCzGdfR0ZBjML4gpoebFT2LD0+eMrbKo/1dZueYHq4j\"}}";
    parse(
        payload,
        PairingVersion.V1_0_0,
        PairingNetwork.TESTNET,
        "BcXzXesuQjLKTgS54rzPBdyJ43IgWdvEXwNJo/ZE49hq8U5eLp/ge+4XQibNAeJS+Eng7AY19hiAIoR3vsTdsyCzGdfR0ZBjML4gpoebFT2LD0+eMrbKo/1dZueYHq4j",
        true); // passphrase=true for V1

    // valid
    payload =
        "{\"pairing\": {\"type\": \"whirlpool.gui\",\"version\": \"1.0.0\",\"network\": \"mainnet\",\"mnemonic\": \"rV2e6YUj33akmh6+k32mjVEE0Amm8XrLRDe4Qvi1WZ1PWAWXHxpuaHwbbXZzzzIlwLnLMNJ8fxtMQMAGR77xew==\"}}";
    parse(
        payload,
        PairingVersion.V1_0_0,
        PairingNetwork.MAINNET,
        "rV2e6YUj33akmh6+k32mjVEE0Amm8XrLRDe4Qvi1WZ1PWAWXHxpuaHwbbXZzzzIlwLnLMNJ8fxtMQMAGR77xew==",
        true); // passphrase=true for V1

    // valid V2
    payload =
        "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"2.0.0\",\"network\":\"testnet\",\"mnemonic\":\"O/fIt9AvelDmz3lVLTzdkvjUtO1MZ1knFPSyPfPNgwMDviVzjAKZSE4mIBvaPazs8sJHZxkyJu09mEgOC4n95TXHCMYWTx3R3MsLfki4WHi77jhZhPDScDExGI9uLlNj\",\"passphrase\":true}}";
    parse(
        payload,
        PairingVersion.V2_0_0,
        PairingNetwork.TESTNET,
        "O/fIt9AvelDmz3lVLTzdkvjUtO1MZ1knFPSyPfPNgwMDviVzjAKZSE4mIBvaPazs8sJHZxkyJu09mEgOC4n95TXHCMYWTx3R3MsLfki4WHi77jhZhPDScDExGI9uLlNj",
        true);

    // valid V2 no passphrase
    payload =
        "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"2.0.0\",\"network\":\"testnet\",\"mnemonic\":\"ih6Jz5eNNdJLdVLTK0W4w23qhr/sT1DUhH46k2nI7j0vp+PKK5LDjYFFY8+SC5Phm9tTBQ47UqFxYvlDElXR0Q==\",\"passphrase\":false}}";
    parse(
        payload,
        PairingVersion.V2_0_0,
        PairingNetwork.TESTNET,
        "ih6Jz5eNNdJLdVLTK0W4w23qhr/sT1DUhH46k2nI7j0vp+PKK5LDjYFFY8+SC5Phm9tTBQ47UqFxYvlDElXR0Q==",
        false);
  }

  @Test
  public void parse_invalid() throws Exception {
    // missing 'pairing'
    try {
      String payload =
          "{\"wrong\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingVersion.V1_0_0, PairingNetwork.TESTNET, "foo", null);
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid type
    try {
      String payload =
          "{\"pairing\":{\"type\":\"foo\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingVersion.V1_0_0, PairingNetwork.TESTNET, "foo", null);
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid version
    try {
      String payload =
          "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"0.0.0\",\"network\":\"testnet\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingVersion.V1_0_0, PairingNetwork.TESTNET, "foo", null);
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid network
    try {
      String payload =
          "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"wrong\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingVersion.V1_0_0, PairingNetwork.TESTNET, "foo", null);
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid mnemonic
    try {
      String payload =
          "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"\"}}";
      parse(payload, PairingVersion.V1_0_0, PairingNetwork.TESTNET, "foo", null);
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }
  }

  private void parse(
      String payload,
      PairingVersion pairingVersion,
      PairingNetwork pairingNetwork,
      String mnemonic,
      Boolean passphrase)
      throws Exception {
    WhirlpoolPairingPayload pairingPayload = WhirlpoolPairingPayload.parse(payload);
    Assert.assertEquals(pairingNetwork, pairingPayload.getPairing().getNetwork());
    Assert.assertEquals(pairingVersion, pairingPayload.getPairing().getVersion());
    Assert.assertEquals(mnemonic, pairingPayload.getPairing().getMnemonic());
    Assert.assertEquals(passphrase, pairingPayload.getPairing().getPassphrase());
  }
}
