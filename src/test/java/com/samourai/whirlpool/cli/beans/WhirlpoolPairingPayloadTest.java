package com.samourai.whirlpool.cli.beans;

import com.samourai.wallet.pairing.payload.PairingNetwork;
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
        PairingNetwork.TESTNET,
        "0qTfDpexBYZ7GM0/F1xCnXctAKLPNcd8+U+GYWNDq7jHxGtsbcfwSeHI0BoVMSm7KrdIgBiKhyUl0XCntfq9drU6nOrtmqo2x1dppnvrLjNI71go2ICospLOtRHiFUac");

    // valid
    payload =
        "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"BcXzXesuQjLKTgS54rzPBdyJ43IgWdvEXwNJo/ZE49hq8U5eLp/ge+4XQibNAeJS+Eng7AY19hiAIoR3vsTdsyCzGdfR0ZBjML4gpoebFT2LD0+eMrbKo/1dZueYHq4j\"}}";
    parse(
        payload,
        PairingNetwork.TESTNET,
        "BcXzXesuQjLKTgS54rzPBdyJ43IgWdvEXwNJo/ZE49hq8U5eLp/ge+4XQibNAeJS+Eng7AY19hiAIoR3vsTdsyCzGdfR0ZBjML4gpoebFT2LD0+eMrbKo/1dZueYHq4j");

    // valid
    payload =
        "{\"pairing\": {\"type\": \"whirlpool.gui\",\"version\": \"1.0.0\",\"network\": \"mainnet\",\"mnemonic\": \"rV2e6YUj33akmh6+k32mjVEE0Amm8XrLRDe4Qvi1WZ1PWAWXHxpuaHwbbXZzzzIlwLnLMNJ8fxtMQMAGR77xew==\"}}";
    parse(
        payload,
        PairingNetwork.MAINNET,
        "rV2e6YUj33akmh6+k32mjVEE0Amm8XrLRDe4Qvi1WZ1PWAWXHxpuaHwbbXZzzzIlwLnLMNJ8fxtMQMAGR77xew==");
  }

  @Test
  public void parse_invalid() throws Exception {
    // missing 'pairing'
    try {
      String payload =
          "{\"wrong\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingNetwork.TESTNET, "foo");
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid type
    try {
      String payload =
          "{\"pairing\":{\"type\":\"foo\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingNetwork.TESTNET, "foo");
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid version
    try {
      String payload =
          "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"0.0.0\",\"network\":\"testnet\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingNetwork.TESTNET, "foo");
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid network
    try {
      String payload =
          "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"wrong\",\"mnemonic\":\"foo\"}}";
      parse(payload, PairingNetwork.TESTNET, "foo");
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }

    // invalid mnemonic
    try {
      String payload =
          "{\"pairing\":{\"type\":\"whirlpool.gui\",\"version\":\"1.0.0\",\"network\":\"testnet\",\"mnemonic\":\"\"}}";
      parse(payload, PairingNetwork.TESTNET, "foo");
      Assert.assertTrue(false);
    } catch (NotifiableException e) {
      // ok
    }
  }

  private void parse(String payload, PairingNetwork pairingNetwork, String mnemonic)
      throws Exception {
    WhirlpoolPairingPayload pairingPayload = WhirlpoolPairingPayload.parse(payload);
    Assert.assertEquals(pairingNetwork, pairingPayload.getPairing().getNetwork());
    Assert.assertEquals(mnemonic, pairingPayload.getPairing().getMnemonic());
  }
}
