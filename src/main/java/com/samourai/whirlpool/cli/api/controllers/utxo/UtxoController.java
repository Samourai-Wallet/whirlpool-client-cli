package com.samourai.whirlpool.cli.api.controllers.utxo;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiTx0CreateRequest;
import com.samourai.whirlpool.cli.api.protocol.rest.ApiTx0CreateResponse;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UtxoController extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;
  @Autowired private Tx0Service tx0Service;

  private WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex) throws Exception {
    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = cliWalletService.getSessionWallet().findUtxo(utxoHash, utxoIndex);
    if (whirlpoolUtxo == null) {
      throw new NotifiableException("Utxo not found: " + utxoHash + ":" + utxoIndex);
    }
    return whirlpoolUtxo;
  }

  @RequestMapping(value = CliApiEndpoint.REST_UTXO_TX0, method = RequestMethod.POST)
  public ApiTx0CreateResponse tx0(
      @RequestHeader HttpHeaders headers,
      @PathVariable("hash") String utxoHash,
      @PathVariable("index") int utxoIndex,
      @RequestBody ApiTx0CreateRequest payload)
      throws Exception {
    checkHeaders(headers);

    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    // find pool
    Pool pool = whirlpoolWallet.findPoolById(payload.poolId);
    if (pool == null) {
      throw new NotifiableException("Pool not found: " + payload.poolId);
    }

    // tx0
    Tx0 tx0 = whirlpoolWallet.tx0(whirlpoolUtxo, pool);
    return new ApiTx0CreateResponse(tx0.getTx().getHashAsString() /*payload.mixsTarget*/);
  }

  @RequestMapping(value = CliApiEndpoint.REST_UTXO_STARTMIX, method = RequestMethod.POST)
  public void startMix(
      @RequestHeader HttpHeaders headers,
      @PathVariable("hash") String utxoHash,
      @PathVariable("index") int utxoIndex)
      throws Exception {
    checkHeaders(headers);

    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    // start mix
    whirlpoolWallet.mixQueue(whirlpoolUtxo);
  }

  @RequestMapping(value = CliApiEndpoint.REST_UTXO_STOPMIX, method = RequestMethod.POST)
  public void stopMix(
      @RequestHeader HttpHeaders headers,
      @PathVariable("hash") String utxoHash,
      @PathVariable("index") int utxoIndex)
      throws Exception {
    checkHeaders(headers);

    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    // start mix
    whirlpoolWallet.mixStop(whirlpoolUtxo);
  }
}
