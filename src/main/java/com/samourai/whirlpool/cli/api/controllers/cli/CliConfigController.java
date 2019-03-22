package com.samourai.whirlpool.cli.api.controllers.cli;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliConfigResponse;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CliConfigController extends AbstractRestController {
  @Autowired private CliConfig cliConfig;
  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = CliApiEndpoint.REST_CLI_CONFIG, method = RequestMethod.GET)
  public ApiCliConfigResponse getCliConfig(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    // check session opened
    cliWalletService.getSessionWallet();

    ApiCliConfigResponse response = new ApiCliConfigResponse(cliConfig);
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_CONFIG, method = RequestMethod.POST)
  public ApiCliConfigResponse setCliConfig(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    // success
    return getCliConfig(headers);
  }
}
