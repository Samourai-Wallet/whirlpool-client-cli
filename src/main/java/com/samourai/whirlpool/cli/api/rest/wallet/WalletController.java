package com.samourai.whirlpool.cli.api.rest.wallet;

import com.samourai.whirlpool.cli.api.rest.AbstractRestController;
import com.samourai.whirlpool.cli.api.rest.protocol.ApiWalletResponse;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletController extends AbstractRestController {
  public static final String ENDPOINT = "/wallet";

  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = ENDPOINT)
  public ApiWalletResponse wallet() throws Exception {
    CliWallet cliWallet = cliWalletService.getCliWallet();
    cliWallet.clearCache(); // TODO
    return new ApiWalletResponse(cliWallet);
  }
}
