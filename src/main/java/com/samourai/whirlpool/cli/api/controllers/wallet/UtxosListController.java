package com.samourai.whirlpool.cli.api.controllers.wallet;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiWalletUtxosResponse;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UtxosListController extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = CliApiEndpoint.REST_UTXOS, method = RequestMethod.GET)
  public ApiWalletUtxosResponse wallet(@RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
    return new ApiWalletUtxosResponse(whirlpoolWallet);
  }
}
