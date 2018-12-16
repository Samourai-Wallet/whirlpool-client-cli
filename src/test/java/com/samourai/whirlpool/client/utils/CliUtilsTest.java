package com.samourai.whirlpool.client.utils;

import com.samourai.whirlpool.client.test.AbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class CliUtilsTest extends AbstractTest {

  @Test
  public void getAddressAt() throws Exception {
    // 72e5d19ee2f56a6db75993b47bbade1011e37a2899b4ee30b8ffdc2b8c8c9f2b: 1 in + 1 out = 191
    Assert.assertEquals(191, CliUtils.estimateTxBytes(1, 1));

    Assert.assertEquals(303, CliUtils.estimateTxBytes(3, 3));
    Assert.assertEquals(404, CliUtils.estimateTxBytes(4, 4));
    Assert.assertEquals(505, CliUtils.estimateTxBytes(5, 5));
  }
}
