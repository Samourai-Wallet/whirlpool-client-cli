package com.samourai.whirlpool.cli.api.rest.wallet;

import com.samourai.whirlpool.cli.api.rest.AbstractRestController;
import com.samourai.whirlpool.cli.api.rest.protocol.ApiWalletResponse;
import com.samourai.whirlpool.cli.api.rest.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletController extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = CliApiEndpoint.REST_WALLET)
  public ApiWalletResponse wallet(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
    whirlpoolWallet.clearCache(); // TODO
    return new ApiWalletResponse(whirlpoolWallet);
  }
}
