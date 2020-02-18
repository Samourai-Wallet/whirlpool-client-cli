package com.samourai.xmanager.client;

import com.samourai.http.client.JavaHttpClient;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.test.AbstractTest;
import com.samourai.xmanager.protocol.XManagerService;
import com.samourai.xmanager.protocol.rest.AddressIndexResponse;
import java.util.Optional;
import org.eclipse.jetty.client.HttpClient;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class XManagerClientTest extends AbstractTest {
  private static final boolean testnet = true;
  private static final long requestTimeout = 5000;

  private XManagerClient xManagerClient;
  private XManagerClient xManagerClientFailing;

  public XManagerClientTest() throws Exception {
    JavaHttpClient httpClient =
        new JavaHttpClient(requestTimeout) {
          @Override
          protected HttpClient computeHttpClient(boolean isRegisterOutput) throws Exception {
            return CliUtils.computeHttpClient(Optional.empty(), "whirlpool-cli/test");
          }
        };
    xManagerClient = new XManagerClient(testnet, false, httpClient);

    JavaHttpClient httpClientFailing =
        new JavaHttpClient(requestTimeout) {
          @Override
          protected HttpClient computeHttpClient(boolean isRegisterOutput) {
            throw new RuntimeException("testing failure");
          }
        };
    xManagerClientFailing = new XManagerClient(testnet, false, httpClientFailing);
  }

  @Test
  public void getAddressOrDefault() throws Exception {
    String address = xManagerClient.getAddressOrDefault(XManagerService.WHIRLPOOL);
    Assertions.assertNotNull(address);
    Assertions.assertNotEquals(XManagerService.WHIRLPOOL.getDefaultAddress(testnet), address);
  }

  @Test
  public void getAddressOrDefault_failure() throws Exception {
    String address = xManagerClientFailing.getAddressOrDefault(XManagerService.WHIRLPOOL);
    Assertions.assertEquals(XManagerService.WHIRLPOOL.getDefaultAddress(testnet), address);
  }

  @Test
  public void getAddressIndexOrDefault() throws Exception {
    AddressIndexResponse addressIndexResponse =
        xManagerClient.getAddressIndexOrDefault(XManagerService.WHIRLPOOL);
    Assertions.assertNotNull(addressIndexResponse);
    Assertions.assertNotEquals(
        XManagerService.WHIRLPOOL.getDefaultAddress(testnet), addressIndexResponse.address);
    Assertions.assertTrue(addressIndexResponse.index > 0);
  }

  @Test
  public void getAddressIndexOrDefault_failure() throws Exception {
    AddressIndexResponse addressIndexResponse =
        xManagerClientFailing.getAddressIndexOrDefault(XManagerService.WHIRLPOOL);
    Assertions.assertEquals(
        XManagerService.WHIRLPOOL.getDefaultAddress(testnet), addressIndexResponse.address);
    Assertions.assertEquals(0, addressIndexResponse.index);
  }

  @Test
  public void verifyAddressIndexResponseOrException() throws Exception {
    Assertions.assertTrue(
        xManagerClient.verifyAddressIndexResponseOrException(
            XManagerService.WHIRLPOOL, "tb1q6m3urxjc8j2l8fltqj93jarmzn0975nnxuymnx", 0));
    Assertions.assertFalse(
        xManagerClient.verifyAddressIndexResponseOrException(
            XManagerService.WHIRLPOOL, "tb1qz84ma37y3d759sdy7mvq3u4vsxlg2qahw3lm23", 0));

    Assertions.assertTrue(
        xManagerClient.verifyAddressIndexResponseOrException(
            XManagerService.WHIRLPOOL, "tb1qcaerxclcmu9llc7ugh65hemqg6raaz4sul535f", 1));
    Assertions.assertFalse(
        xManagerClient.verifyAddressIndexResponseOrException(
            XManagerService.WHIRLPOOL, "tb1qcfgn9nlgxu0ycj446prdkg0p36qy5a39pcf74v", 1));
  }

  @Test
  public void verifyAddressIndexResponseOrException_failure() throws Exception {
    try {
      xManagerClientFailing.verifyAddressIndexResponseOrException(
          XManagerService.WHIRLPOOL, "tb1qcfgn9nlgxu0ycj446prdkg0p36qy5a39pcf74v", 0);
      Assert.assertTrue(false); // exception expected
    } catch (RuntimeException e) {
      // ok
    }
  }
}
