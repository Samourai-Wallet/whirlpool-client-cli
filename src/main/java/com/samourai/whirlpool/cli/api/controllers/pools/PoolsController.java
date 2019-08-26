package com.samourai.whirlpool.cli.api.controllers.pools;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiPoolsResponse;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
public class PoolsController extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = CliApiEndpoint.REST_POOLS, method = RequestMethod.GET)
  public ApiPoolsResponse pools(
      @RequestParam(value = "tx0FeeTarget", defaultValue = "BLOCKS_24")
          Tx0FeeTarget tx0FeeTarget, // Tx0FeeTarget.MIN
      @RequestHeader HttpHeaders headers)
      throws Exception {
    checkHeaders(headers);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();
    Collection<Pool> pools = whirlpoolWallet.getPools(false);
    return new ApiPoolsResponse(pools, tx0FeeTarget, whirlpoolWallet);
  }
}
