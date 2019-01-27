package com.samourai.whirlpool.cli.api.rest.protocol;

import com.samourai.whirlpool.cli.wallet.CliWallet;
import java.util.Collection;
import java.util.stream.Collectors;

public class ApiWalletResponse {
  private Collection<ApiUtxo> utxosDeposit;
  private Collection<ApiUtxo> utxosPremix;
  private Collection<ApiUtxo> utxosPostmix;

  public ApiWalletResponse(CliWallet cliWallet) throws Exception {
    this.utxosDeposit =
        cliWallet
            .getUtxosDeposit()
            .stream()
            .map(utxo -> new ApiUtxo(utxo))
            .collect(Collectors.toList());
    this.utxosPremix =
        cliWallet
            .getUtxosPremix()
            .stream()
            .map(utxo -> new ApiUtxo(utxo))
            .collect(Collectors.toList());
    this.utxosPostmix =
        cliWallet
            .getUtxosPostmix()
            .stream()
            .map(utxo -> new ApiUtxo(utxo))
            .collect(Collectors.toList());
  }

  public Collection<ApiUtxo> getUtxosDeposit() {
    return utxosDeposit;
  }

  public Collection<ApiUtxo> getUtxosPremix() {
    return utxosPremix;
  }

  public Collection<ApiUtxo> getUtxosPostmix() {
    return utxosPostmix;
  }
}
