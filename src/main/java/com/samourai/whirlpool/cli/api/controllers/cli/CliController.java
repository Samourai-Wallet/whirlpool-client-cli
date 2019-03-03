package com.samourai.whirlpool.cli.api.controllers.cli;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitResponse;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliStatusResponse;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.cli.services.CliConfigService;
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

  @RequestMapping(value = CliApiEndpoint.REST_CLI, method = RequestMethod.GET)
  public ApiCliStatusResponse status(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    CliStatus cliStatus = cliConfigService.getCliStatus();
    ApiCliStatusResponse response = new ApiCliStatusResponse(cliStatus);
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
}
