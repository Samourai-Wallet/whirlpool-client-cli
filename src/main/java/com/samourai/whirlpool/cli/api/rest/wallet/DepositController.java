package com.samourai.whirlpool.cli.api.rest.wallet;

import com.samourai.whirlpool.cli.api.rest.AbstractRestController;
import com.samourai.whirlpool.cli.api.rest.protocol.ApiDepositResponse;
import com.samourai.whirlpool.cli.api.rest.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepositController extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = CliApiEndpoint.REST_WALLET_DEPOSIT)
  public ApiDepositResponse wallet(
      @RequestParam(value = "increment", defaultValue = "false") boolean increment,
      @RequestHeader HttpHeaders headers)
      throws Exception {
    checkHeaders(headers);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
    String depositAddress = whirlpoolWallet.getDepositAddress(increment);
    return new ApiDepositResponse(depositAddress);
  }
}
