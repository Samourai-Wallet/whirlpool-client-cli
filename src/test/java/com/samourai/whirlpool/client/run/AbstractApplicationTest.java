package com.samourai.whirlpool.client.run;

import com.samourai.whirlpool.client.utils.CaptureStream;
import java.io.PrintStream;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class AbstractApplicationTest {

  // recipe obtain chunk amused split second disorder budget okay verb border rifle",
  // vpub5Yw9AZRFizDCncCuuUDr1VgvXzYB6QUQ6bLfbQfp9C8ZVLjs56QvqQ2WbmcaU7FAY2cfcse3NaLyhXn2xVfZo3eec2q6Haxr3o8G796qEYr",

  // elite pause shift celery boost regular clay soldier mercy rebuild depth avoid",
  // vpub5Yg9j2zBK4pQEQ779mJwR3GxaQ2NRuvugjL26jZNBWKRDfvU4Dy3tbmuF6gbfssc2XLg8Bz7XA2pwZjkmDmmsBdYJXpnBw3vaVCHAjQwhn2",
  protected static final String VPUB =
      "vpub5YW6Bhq66LvLJuKPetDwyg83NvrF5i9SYRwXvyWYFXxDjoYXYeFrfpwwcATN9NQSqpqz7kXg7FMVCkn87VuwTKpGzTLSUb6LctfRVpALYHh";
  protected static final String SEED_WORDS =
      "leisure mix glove infant admit multiply rib harbor burden once loop deposit";
  protected static final String SEED_PASSPHRASE = "whirlpool";
  protected static final String SERVER = "127.0.0.1:8080";
  protected static final String RPC_CLIENT_URL = "http://user:password@host:port";

  private CaptureStream outContent;
  private CaptureStream errContent;
  private PrintStream outOrig = System.out;
  private PrintStream errOrig = System.err;

  protected void captureSystem() {
    outContent = new CaptureStream(System.out);
    errContent = new CaptureStream(System.err);
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  protected void resetSystem() {
    System.setOut(outOrig);
    System.setErr(errOrig);
  }

  protected String getOut() {
    return outContent.toString();
  }

  protected String getErr() {
    return errContent.toString();
  }

  public void setup() {
    captureSystem();
  }

  public void tearDown() {
    resetSystem();
  }
}
