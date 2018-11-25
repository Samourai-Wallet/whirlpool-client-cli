package com.samourai.whirlpool.client.run;

import com.samourai.whirlpool.client.Application;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationWalletTest extends AbstractApplicationTest {
  private static final String SEED_WORDS =
      "leisure mix glove infant admit multiply rib harbor burden once loop deposit";
  private static final String SEED_PASSPHRASE = "whirlpool4";
  private static final String SERVER = "pool.whirl.mx:8081";
  private static final String RPC_CLIENT_URL =
      "http://zeroleak:833b09863f0ef98435382dfbe942352551124%e5316623659e3ba8__59bb911d562@212.129.55.26:18332";

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
          "--iteration-delay=60"
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
          "--server=" + SERVER
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
