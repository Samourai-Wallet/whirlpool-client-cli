package com.samourai.whirlpool.cli.api.protocol;

import com.samourai.whirlpool.client.wallet.beans.MixOrchestratorState;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxoPriorityComparator;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolWalletState;
import java.util.Collection;
import java.util.stream.Collectors;

public class ApiWalletStateResponse {
  private boolean started;

  private int nbMixing;
  private int maxClients;
  private int nbIdle;
  private int nbQueued;
  private Collection<ApiUtxo> threads;

  public ApiWalletStateResponse(WhirlpoolWalletState whirlpoolWalletState) {
    this.started = whirlpoolWalletState.isStarted();

    MixOrchestratorState mixState = whirlpoolWalletState.getMixState();
    this.nbMixing = mixState.getNbMixing();
    this.maxClients = mixState.getMaxClients();
    this.nbIdle = mixState.getNbIdle();
    this.nbQueued = mixState.getNbQueued();
    this.threads =
        mixState
            .getUtxosMixing()
            .stream()
            .sorted(new WhirlpoolUtxoPriorityComparator())
            .map(whirlpoolUtxo -> new ApiUtxo(whirlpoolUtxo))
            .collect(Collectors.toList());
  }

  public boolean isStarted() {
    return started;
  }

  public int getNbMixing() {
    return nbMixing;
  }

  public int getMaxClients() {
    return maxClients;
  }

  public int getNbIdle() {
    return nbIdle;
  }

  public int getNbQueued() {
    return nbQueued;
  }

  public Collection<ApiUtxo> getThreads() {
    return threads;
  }
}
