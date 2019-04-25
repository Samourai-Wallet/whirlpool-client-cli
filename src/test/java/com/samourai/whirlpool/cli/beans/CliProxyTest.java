package com.samourai.whirlpool.cli.beans;

import org.junit.Assert;
import org.junit.Test;

public class CliProxyTest {

  @Test
  public void testValidate() throws Exception {
    // valid
    Assert.assertTrue(CliProxy.validate("http://localhost:8080"));
    Assert.assertTrue(CliProxy.validate("socks://localhost:9050"));

    // invalid
    Assert.assertFalse(CliProxy.validate("foo://localhost:9050")); // invalid protocol
    Assert.assertFalse(CliProxy.validate("http://localhost")); // missing port
    Assert.assertFalse(CliProxy.validate("localhost:8080")); // missing protocol
  }
}
