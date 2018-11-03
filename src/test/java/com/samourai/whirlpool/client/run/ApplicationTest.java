package com.samourai.whirlpool.client.run;

import com.samourai.whirlpool.client.Application;
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
public class ApplicationTest extends AbstractApplicationTest {

  @Before
  @Override
  public void setup() {
    super.setup();
  }

  @After
  @Override
  public void tearDown() {
    super.tearDown();
  }

  @Test
  public void runListPools() {
    String[] args = new String[] {"--network=test", "--debug", "--server=" + SERVER};
    ApplicationArguments appArgs = new DefaultApplicationArguments(args);

    new Application().run(appArgs);

    Assert.assertTrue(getOut().contains(" • Retrieving pools..."));
    Assert.assertTrue(getErr().isEmpty());
  }

  @Test
  public void runWhirlpool() {
    String[] args =
        new String[] {
          "--network=test",
          "--utxo=733a1bcb4145e3dd0ea3e6709bef9504fd252c9a26b254508539e3636db659c2-1",
          "--utxo-key=cUe6J7Fs5mxg6jLwXE27xcDpaTPXfQZ9oKDbxs5PP6EpYMFHab2T",
          "--utxo-balance=1000102",
          "--seed-passphrase=w0",
          "--seed-words=all all all all all all all all all all all all",
          "--mixs=5",
          "--debug",
          "--pool=1btc",
          "--test-mode",
          "--server=" + SERVER
        };
    ApplicationArguments appArgs = new DefaultApplicationArguments(args);

    captureSystem();
    new Application().run(appArgs); // TODO mock server
    resetSystem();

    Assert.assertTrue(getOut().contains(" • connecting to "));
    Assert.assertTrue(getErr().isEmpty());
  }
}
