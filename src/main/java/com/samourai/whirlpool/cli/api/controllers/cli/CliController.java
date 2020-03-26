package com.samourai.whirlpool.cli.api.controllers.cli;

import com.samourai.whirlpool.cli.Application;
import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliInitResponse;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliLoginRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiCliStateResponse;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.cli.beans.WhirlpoolPairingPayload;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.lang.invoke.MethodHandles;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CliController extends AbstractRestController {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired private CliConfigService cliConfigService;
  @Autowired private CliWalletService cliWalletService;
  @Autowired private CliConfig cliConfig;
  @Autowired private TaskExecutor taskExecutor;

  @RequestMapping(value = CliApiEndpoint.REST_CLI, method = RequestMethod.GET)
  public ApiCliStateResponse state(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);
    ApiCliStateResponse response =
        new ApiCliStateResponse(
            cliWalletService.getCliState(),
            cliConfig.getServer(),
            cliConfig.computeServerUrl(),
            cliConfig.getDojo().getUrl(),
            cliConfig.getTor(),
            cliConfig.isDojoEnabled());
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_INIT, method = RequestMethod.POST)
  public ApiCliInitResponse init(
      @RequestHeader HttpHeaders headers, @Valid @RequestBody ApiCliInitRequest payload)
      throws Exception {
    checkHeaders(headers);

    // security: check not already initialized
    if (!CliStatus.NOT_INITIALIZED.equals(cliConfigService.getCliStatus())) {
      throw new NotifiableException("CLI is already initialized.");
    }

    // init
    String pairingPayload = payload.pairingPayload;
    boolean tor = payload.tor;
    boolean dojo = payload.dojo;
    WhirlpoolPairingPayload pairing = cliConfigService.parsePairingPayload(pairingPayload);
    String apiKey = cliConfigService.initialize(pairing, tor, dojo);

    ApiCliInitResponse response = new ApiCliInitResponse(apiKey);

    // restart CLI *AFTER* response reply
    taskExecutor.execute(
        new Runnable() {
          @Override
          public void run() {
            try {
              Thread.sleep(1000);
            } catch (Exception e) {
              log.error("", e);
            }
            Application.restart();
          }
        });
    return response;
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_LOGIN, method = RequestMethod.POST)
  public ApiCliStateResponse login(
      @RequestHeader HttpHeaders headers, @Valid @RequestBody ApiCliLoginRequest payload)
      throws Exception {
    checkHeaders(headers);

    cliWalletService.openWallet(payload.seedPassphrase).start();

    // success
    return state(headers);
  }

  @RequestMapping(value = CliApiEndpoint.REST_CLI_LOGOUT, method = RequestMethod.POST)
  public ApiCliStateResponse logout(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    cliWalletService.closeWallet();

    // success
    return state(headers);
  }
}
