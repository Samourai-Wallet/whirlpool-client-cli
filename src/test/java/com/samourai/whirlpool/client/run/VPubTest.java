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
public class VPubTest extends AbstractApplicationTest {

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
    public void runVPubLoop() {
        String[] args = new String[]{
                "--network=test",
                "--seed-passphrase=" + SEED_PASSPHRASE,
                "--vpub=" + VPUB,
                "--rpc-client-url=" + RPC_CLIENT_URL,
                "--seed-words=" + SEED_WORDS,
                "--debug",
                "--pool=0.01btc",
                "--test-mode",
                "--server=" + SERVER,
        };
        ApplicationArguments appArgs = new DefaultApplicationArguments(args);
        new Application().run(appArgs);

        Assert.assertTrue(getErr().isEmpty());
    }

    @Test
    public void runVpubTx0WithRpcClientUrl() {
        String[] args = new String[]{
                "--network=test",
                "--seed-passphrase=" + SEED_PASSPHRASE,
                "--vpub=" + VPUB,
                "--rpc-client-url=" + RPC_CLIENT_URL,
                "--seed-words=" + SEED_WORDS,
                "--tx0=20",
                "--debug",
                "--pool=0.01btc",
                "--server=" + SERVER
        };
        ApplicationArguments appArgs = new DefaultApplicationArguments(args);
        new Application().run(appArgs);

        Assert.assertTrue(getErr().isEmpty());
    }

    @Test
    public void runVpubTx0WithoutRpcClientUrl() {
        String[] args = new String[]{
                "--network=test",
                "--seed-passphrase=" + SEED_PASSPHRASE,
                "--vpub=" + VPUB,
                "--seed-words=" + SEED_WORDS,
                "--tx0=20",
                "--debug",
                "--pool=0.01btc",
                "--server=" + SERVER
        };
        ApplicationArguments appArgs = new DefaultApplicationArguments(args);
        new Application().run(appArgs);

        Assert.assertTrue(getErr().isEmpty());
    }
}