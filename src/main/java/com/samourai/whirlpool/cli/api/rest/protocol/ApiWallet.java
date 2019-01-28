package com.samourai.whirlpool.cli.api.rest.protocol;

import com.samourai.api.client.beans.UnspentResponse.UnspentOutput;
import java.util.Collection;
import java.util.stream.Collectors;

public class ApiWallet {
  private Collection<ApiUtxo> utxos;
  private long balance;

  public ApiWallet(Collection<UnspentOutput> utxos) {
    this.utxos = utxos.stream().map(utxo -> new ApiUtxo(utxo)).collect(Collectors.toList());
    this.balance = utxos.stream().mapToLong(utxo -> utxo.value).sum();
  }

  public Collection<ApiUtxo> getUtxos() {
    return utxos;
  }

  public long getBalance() {
    return balance;
  }
}
