package com.samourai.whirlpool.cli.api.controllers.cli;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.beans.ApiEncrypted;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitResponse;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliLoginRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliStatusResponse;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.cli.beans.Encrypted;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.exception.NotifiableException;
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
    String cliMessage = cliConfigService.getCliMessage();
    boolean loggedIn = cliWalletService.hasSessionWallet();
    ApiCliStatusResponse response = new ApiCliStatusResponse(cliStatus, cliMessage, loggedIn);
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_INIT, method = RequestMethod.POST)
  public ApiCliInitResponse init(
      @RequestHeader HttpHeaders headers, @RequestBody ApiCliInitRequest payload) throws Exception {
    checkHeaders(headers);

    // security: check not already initialized
    if (!CliStatus.NOT_INITIALIZED.equals(cliConfigService.getCliStatus())) {
      throw new NotifiableException("CLI is already initialized.");
    }

    // init
    ApiEncrypted sw = payload.encryptedSeedWords;
    Encrypted seedWordsEncrypted = new Encrypted(sw.iv, sw.salt, sw.ct);
    String apiKey = cliConfigService.initialize(seedWordsEncrypted);

    ApiCliInitResponse response = new ApiCliInitResponse(apiKey);
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_LOGIN, method = RequestMethod.POST)
  public ApiCliStatusResponse login(
      @RequestHeader HttpHeaders headers, @RequestBody ApiCliLoginRequest payload)
      throws Exception {
    checkHeaders(headers);

    cliWalletService.openWallet(payload.seedPassphrase).start();

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
