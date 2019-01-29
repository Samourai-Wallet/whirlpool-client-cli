package com.samourai.whirlpool.cli.api.rest.wallet;

import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.api.rest.AbstractRestController;
import com.samourai.whirlpool.cli.api.rest.protocol.ApiDepositResponse;
import com.samourai.whirlpool.cli.api.rest.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import org.bitcoinj.core.NetworkParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepositController extends AbstractRestController {
  @Autowired private CliWalletService cliWalletService;
  @Autowired private Bech32UtilGeneric bech32Util;
  @Autowired private NetworkParameters params;

  @RequestMapping(value = CliApiEndpoint.REST_WALLET_DEPOSIT)
  public ApiDepositResponse wallet(
      @RequestParam(value = "increment", defaultValue = "false") boolean increment,
      @RequestHeader HttpHeaders headers)
      throws Exception {
    checkHeaders(headers);
    CliWallet cliWallet = cliWalletService.getCliWallet();
    String depositAddress =
        bech32Util.toBech32(cliWallet.getDepositWallet().getNextAddress(increment), params);
    return new ApiDepositResponse(depositAddress);
  }
}
