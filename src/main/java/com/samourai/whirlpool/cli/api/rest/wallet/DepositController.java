package com.samourai.whirlpool.cli.api.rest.wallet;

import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.api.rest.protocol.ApiDepositResponse;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import org.bitcoinj.core.NetworkParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepositController {
  private static final String ENDPOINT = "/wallet/deposit";

  @Autowired private CliWalletService cliWalletService;
  @Autowired private Bech32UtilGeneric bech32Util;
  @Autowired private NetworkParameters params;

  @RequestMapping(value = ENDPOINT)
  public ApiDepositResponse wallet(
      @RequestParam(value = "increment", defaultValue = "false") boolean increment)
      throws Exception {
    CliWallet cliWallet = cliWalletService.getCliWallet();
    String depositAddress =
        bech32Util.toBech32(cliWallet.getDepositWallet().getNextAddress(increment), params);
    return new ApiDepositResponse(depositAddress);
  }
}
