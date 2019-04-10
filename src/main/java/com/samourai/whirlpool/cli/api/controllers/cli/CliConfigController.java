package com.samourai.whirlpool.cli.api.controllers.cli;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.beans.ApiCliConfig;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliConfigRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliConfigResponse;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CliConfigController extends AbstractRestController {
  @Autowired private CliConfig cliConfig;
  @Autowired private CliConfigService cliConfigService;

  @RequestMapping(value = CliApiEndpoint.REST_CLI_CONFIG, method = RequestMethod.GET)
  public ApiCliConfigResponse getCliConfig(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    ApiCliConfigResponse response = new ApiCliConfigResponse(cliConfig);
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_CONFIG, method = RequestMethod.POST)
  public void setCliConfig(
      @RequestHeader HttpHeaders headers, @RequestBody ApiCliConfigRequest payload)
      throws Exception {
    checkHeaders(headers);

    ApiCliConfig apiCliConfig = payload.getConfig();
    cliConfigService.setApiConfig(apiCliConfig);
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_CONFIG, method = RequestMethod.DELETE)
  public void resetCliConfig(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    cliConfigService.resetConfiguration();
  }
}
