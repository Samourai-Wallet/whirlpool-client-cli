package com.samourai.whirlpool.cli.api.controllers.cli;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitResponse;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliLoginRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliStatusResponse;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CliController extends AbstractRestController {
  @Autowired private CliConfigService cliConfigService;
  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = CliApiEndpoint.REST_CLI, method = RequestMethod.GET)
  public ApiCliStatusResponse status(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    CliStatus cliStatus = cliConfigService.getCliStatus();
    boolean loggedIn = cliWalletService.hasSessionWallet();
    ApiCliStatusResponse response = new ApiCliStatusResponse(cliStatus, loggedIn);
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_INIT, method = RequestMethod.POST)
  public ApiCliInitResponse init(
      @RequestHeader HttpHeaders headers, @RequestBody ApiCliInitRequest payload) throws Exception {
    checkHeaders(headers);

    // init
    String apiKey = cliConfigService.initialize(payload.encryptedSeedWords);

    ApiCliInitResponse response = new ApiCliInitResponse(apiKey);
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_LOGIN, method = RequestMethod.POST)
  public ApiCliStatusResponse login(
      @RequestHeader HttpHeaders headers, @RequestBody ApiCliLoginRequest payload)
      throws Exception {
    checkHeaders(headers);

    cliWalletService.openWallet(payload.seedPassphrase);

    // success
    return status(headers);
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_LOGOUT, method = RequestMethod.POST)
  public ApiCliStatusResponse logout(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    cliWalletService.closeWallet();

    // success
    return status(headers);
  }
}
