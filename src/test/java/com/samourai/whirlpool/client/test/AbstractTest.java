package com.samourai.whirlpool.client.test;

import org.junit.After;
import org.junit.Before;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class AbstractTest {
  @Before
  public void setup() throws Exception {}

  @After
  public void tearDown() throws Exception {}
}
