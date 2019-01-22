package com.samourai.whirlpool.client.run;

import com.samourai.whirlpool.cli.Application;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore
public class ApplicationWalletTest extends AbstractApplicationTest {
  private static final String SEED_WORDS = "all all all all all all all all all all all all";
  private static final String SEED_PASSPHRASE = "whirlpool";
  protected static final String SERVER = "127.0.0.1:8080";
  protected static final String RPC_CLIENT_URL = "http://user:password@host:port";

  @Before
  @Override
  public void setup() throws Exception {
    super.setup();
  }

  @After
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  @Test
  public void runLoopWallet() {
    String[] args =
        new String[] {
          "--network=test",
          "--seed-passphrase=" + SEED_PASSPHRASE,
          "--rpc-client-url=" + RPC_CLIENT_URL,
          "--seed-words=" + SEED_WORDS,
          "--debug",
          "--pool=0.01btc",
          "--server=" + SERVER,
          "--clients=5",
          "--postmix-index=0",
          "--client-delay=1",
          "--tor=false"
        };
    ApplicationArguments appArgs = new DefaultApplicationArguments(args);
    new Application().run(appArgs);

    Assert.assertTrue(getErr().isEmpty());
  }

  @Test
  public void runTx0WithRpcClient() {
    String[] args =
        new String[] {
          "--network=test",
          "--seed-passphrase=" + SEED_PASSPHRASE,
          "--rpc-client-url=" + RPC_CLIENT_URL,
          "--seed-words=" + SEED_WORDS,
          "--tx0=1",
          "--debug",
          "--pool=0.01btc",
          "--server=" + SERVER,
          "--tor=false"
        };
    ApplicationArguments appArgs = new DefaultApplicationArguments(args);
    new Application().run(appArgs);

    Assert.assertTrue(getErr().isEmpty());
  }

  @Test
  public void runTx0WithoutRpcClient() {
    String[] args =
        new String[] {
          "--network=test",
          "--seed-passphrase=" + SEED_PASSPHRASE,
          "--seed-words=" + SEED_WORDS,
          "--tx0=1",
          "--debug",
          "--pool=0.01btc",
          "--server=" + SERVER
        };
    ApplicationArguments appArgs = new DefaultApplicationArguments(args);
    new Application().run(appArgs);

    Assert.assertTrue(getErr().isEmpty());
  }

  @Test
  public void runAggregatePostmix() {
    String[] args =
        new String[] {
          "--network=test",
          "--seed-passphrase=" + SEED_PASSPHRASE,
          "--rpc-client-url=" + RPC_CLIENT_URL,
          "--seed-words=" + SEED_WORDS,
          "--aggregate-postmix=tb1qkxzjh0u8a84c3cqpcu83c4mrhh3vxkgnyp4wa8",
          "--debug",
          "--pool=0.01btc",
          "--server=" + SERVER
        };
    ApplicationArguments appArgs = new DefaultApplicationArguments(args);
    new Application().run(appArgs);

    Assert.assertTrue(getErr().isEmpty());
  }
}
