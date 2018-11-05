package com.samourai.whirlpool.client.run;

import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.whirlpool.client.test.CaptureStream;
import java.io.PrintStream;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class AbstractApplicationTest extends AbstractTest {

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

  public void setup() throws Exception {
    super.setup();
    captureSystem();
  }

  public void tearDown() throws Exception {
    super.tearDown();
    resetSystem();
  }
}
