package com.samourai.whirlpool.cli.api.controllers.utxo;

import com.samourai.whirlpool.cli.api.controllers.AbstractRestController;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.api.protocol.beans.ApiUtxo;
import com.samourai.whirlpool.cli.api.protocol.rest.*;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.wallet.WhirlpoolWallet;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java8.util.Lists;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
public class UtxoController extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;

  private WhirlpoolUtxo findUtxo(String utxoHash, int utxoIndex) throws Exception {
    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = cliWalletService.getSessionWallet().findUtxo(utxoHash, utxoIndex);
    if (whirlpoolUtxo == null) {
      throw new NotifiableException("Utxo not found: " + utxoHash + ":" + utxoIndex);
    }
    return whirlpoolUtxo;
  }

  @RequestMapping(value = CliApiEndpoint.REST_UTXO_CONFIGURE, method = RequestMethod.POST)
  public ApiUtxo configureUtxo(
      @RequestHeader HttpHeaders headers,
      @PathVariable("hash") String utxoHash,
      @PathVariable("index") int utxoIndex,
      @Valid @RequestBody ApiUtxoConfigureRequest payload)
      throws Exception {
    checkHeaders(headers);

    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    // configure pool
    whirlpoolWallet.setPool(whirlpoolUtxo, payload.poolId);

    // configure mixsTarget
    whirlpoolWallet.setMixsTarget(whirlpoolUtxo, payload.mixsTarget);

    int mixsTargetMin = whirlpoolWallet.getConfig().getMixsTarget();
    ApiUtxo apiUtxo = new ApiUtxo(whirlpoolUtxo, mixsTargetMin);
    return apiUtxo;
  }

  @RequestMapping(value = CliApiEndpoint.REST_UTXO_TX0_PREVIEW, method = RequestMethod.POST)
  public ApiTx0PreviewResponse tx0Preview(
      @RequestHeader HttpHeaders headers,
      @PathVariable("hash") String utxoHash,
      @PathVariable("index") int utxoIndex,
      @Valid @RequestBody ApiTx0PreviewRequest payload)
      throws Exception {
    checkHeaders(headers);

    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    Pool pool = whirlpoolWallet.findPoolById(payload.poolId);
    if (pool == null) {
      throw new NotifiableException("poolId is not valid");
    }

    // tx0 preview
      Tx0Config tx0Config = whirlpoolWallet.getTx0Config(pool);
    Tx0Preview tx0Preview =
        whirlpoolWallet.tx0Preview(
            Lists.of(whirlpoolUtxo), pool, tx0Config, payload.feeTarget);
    return new ApiTx0PreviewResponse(tx0Preview);
  }

  @RequestMapping(value = CliApiEndpoint.REST_UTXO_TX0, method = RequestMethod.POST)
  public ApiTx0Response tx0(
      @RequestHeader HttpHeaders headers,
      @PathVariable("hash") String utxoHash,
      @PathVariable("index") int utxoIndex,
      @Valid @RequestBody ApiTx0Request payload)
      throws Exception {
    checkHeaders(headers);

    // find utxo
    WhirlpoolUtxo whirlpoolUtxo = findUtxo(utxoHash, utxoIndex);
    WhirlpoolWallet whirlpoolWallet = cliWalletService.getSessionWallet();

    // override utxo settings
    if (payload.mixsTarget != null && payload.mixsTarget > 0) {
      whirlpoolWallet.setMixsTarget(whirlpoolUtxo, payload.mixsTarget);
    }

    Pool pool = whirlpoolWallet.findPoolById(payload.poolId);
    if (pool == null) {
      throw new NotifiableException("poolId is not valid");
    }

    // tx0
      Tx0Config tx0Config = whirlpoolWallet.getTx0Config(pool);
    Tx0 tx0 =
        whirlpoolWallet.tx0(
            Lists.of(whirlpoolUtxo), pool, payload.feeTarget, tx0Config);
    return new ApiTx0Response(tx0);
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

    // stop mix
    whirlpoolWallet.mixStop(whirlpoolUtxo);
  }
}
