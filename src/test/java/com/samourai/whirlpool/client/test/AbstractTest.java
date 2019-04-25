package com.samourai.whirlpool.client.test;

import com.samourai.whirlpool.cli.utils.CliUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.test.context.ActiveProfiles;

@SpringBootConfiguration
@ActiveProfiles(CliUtils.SPRING_PROFILE_TESTING)
public class AbstractTest {
  @Before
  public void setup() throws Exception {}

  @After
  public void tearDown() throws Exception {}
}
