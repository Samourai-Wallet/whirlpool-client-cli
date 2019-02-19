package com.samourai.whirlpool.cli.api.controllers.tx0;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiTx0CreateRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiTx0CreateResponse;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiTx0PoolsResponse;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Tx0Controller extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;

  @RequestMapping(value = CliApiEndpoint.REST_TX0_POOLS, method = RequestMethod.GET)
  public ApiTx0PoolsResponse pools(
      @RequestParam("value") long value, @RequestHeader HttpHeaders headers) throws Exception {
    checkHeaders(headers);

    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    // find pools
    Collection<Pool> pools = whirlpoolWallet.findTx0Pools(value, 1, true);
    return new ApiTx0PoolsResponse(pools);
  }

  @RequestMapping(value = CliApiEndpoint.REST_TX0_CREATE, method = RequestMethod.POST)
  public ApiTx0CreateResponse create(
      @RequestBody ApiTx0CreateRequest payload, @RequestHeader HttpHeaders headers)
      throws Exception {
    checkHeaders(headers);

    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = whirlpoolWallet.findUtxo(payload.hash, payload.index);
    if (whirlpoolUtxo == null) {
      throw new NotifiableException("Utxo not found: " + payload.hash + ":" + payload.index);
    }

    // find pool
    Pool pool = whirlpoolWallet.getPools().findPoolById(payload.poolId);
    if (pool == null) {
      throw new NotifiableException("Pool not found: " + payload.poolId);
    }

    // tx0
    Tx0 tx0 = whirlpoolWallet.tx0(whirlpoolUtxo, pool);
    return new ApiTx0CreateResponse(tx0.getTx().getHashAsString() /*payload.mixsTarget*/);
  }
}
