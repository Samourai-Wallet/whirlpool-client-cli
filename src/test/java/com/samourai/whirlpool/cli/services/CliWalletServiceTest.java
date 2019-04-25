package com.samourai.whirlpool.cli.services;

import com.samourai.whirlpool.client.test.AbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CliWalletServiceTest extends AbstractTest {
  @Autowired private CliWalletService cliWalletService;

  @Override
  public void setup() throws Exception {
    super.setup();
  }

  @Test
  public void decryptSeedWords() throws Exception {
    String seedWordsEncrypted;
    String passphrase;

    seedWordsEncrypted =
        "0qTfDpexBYZ7GM0/F1xCnXctAKLPNcd8+U+GYWNDq7jHxGtsbcfwSeHI0BoVMSm7KrdIgBiKhyUl0XCntfq9drU6nOrtmqo2x1dppnvrLjNI71go2ICospLOtRHiFUac";
    passphrase = "test";
    Assert.assertTrue(
        cliWalletService.decryptSeedWords(seedWordsEncrypted, passphrase).split(" ").length == 12);

    seedWordsEncrypted =
        "BcXzXesuQjLKTgS54rzPBdyJ43IgWdvEXwNJo/ZE49hq8U5eLp/ge+4XQibNAeJS+Eng7AY19hiAIoR3vsTdsyCzGdfR0ZBjML4gpoebFT2LD0+eMrbKo/1dZueYHq4j";
    passphrase = "test1";
    Assert.assertTrue(
        cliWalletService.decryptSeedWords(seedWordsEncrypted, passphrase).split(" ").length == 12);

    seedWordsEncrypted =
        "rV2e6YUj33akmh6+k32mjVEE0Amm8XrLRDe4Qvi1WZ1PWAWXHxpuaHwbbXZzzzIlwLnLMNJ8fxtMQMAGR77xew==";
    passphrase = "test1";
    Assert.assertTrue(
        cliWalletService.decryptSeedWords(seedWordsEncrypted, passphrase).split(" ").length == 12);
  }
}
