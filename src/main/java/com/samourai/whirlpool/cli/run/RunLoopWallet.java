package com.samourai.whirlpool.cli.run;

import com.samourai.api.client.SamouraiApi;
import com.samourai.api.client.beans.UnspentResponse;
import com.samourai.wallet.client.Bip84ApiWallet;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.services.CliWalletService;
import com.samourai.whirlpool.cli.services.WalletAggregateService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.cli.wallet.CliWallet;
import com.samourai.whirlpool.client.exception.EmptyWalletException;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.tx0.Tx0Service;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtxo;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunLoopWallet {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int SLEEP_LOOPWALLET_ON_ERROR = 30000;
  private static final int OUTPUTS_PER_TX0 = 5;

  private CliTorClientService torClientService;
  private Bech32UtilGeneric bech32Util;
  private WhirlpoolClientConfig whirlpoolClientConfig;
  private SamouraiApi samouraiApi;
  private CliWalletService cliWalletService;
  private WalletAggregateService walletAggregateService;
  private Tx0Service tx0Service;

  public RunLoopWallet(
      CliTorClientService torClientService,
      Bech32UtilGeneric bech32Util,
      WhirlpoolClientConfig whirlpoolClientConfig,
      SamouraiApi samouraiApi,
      CliWalletService cliWalletService,
      WalletAggregateService walletAggregateService,
      Tx0Service tx0Service) {
    this.torClientService = torClientService;
    this.bech32Util = bech32Util;
    this.whirlpoolClientConfig = whirlpoolClientConfig;
    this.samouraiApi = samouraiApi;
    this.cliWalletService = cliWalletService;
    this.walletAggregateService = walletAggregateService;
    this.tx0Service = tx0Service;
  }

  public void run(
      Pool pool, int iterationDelay, int nbClients, int clientDelay, boolean isAutoAggregatePostmix)
      throws Exception {
    int i = 1;
    int errors = 0;
    while (true) {
      Bip84ApiWallet premixWallet = cliWalletService.getCliWallet().getPremixWallet();
      Bip84ApiWallet postmixWallet = cliWalletService.getCliWallet().getPostmixWallet();
      try {
        boolean success =
            runLoopIteration(premixWallet, pool, nbClients, clientDelay, isAutoAggregatePostmix);
        if (!success) {
          throw new NotifiableException("Iteration failed");
        }

        log.info(
            " ✔ Cycle #"
                + i
                + " SUCCESS. Next cycle in "
                + iterationDelay
                + "s...  (total success: "
                + (i - errors)
                + ", errors: "
                + errors
                + ", postmixIndex: "
                + postmixWallet.getIndexHandler().get()
                + ")");
        if (iterationDelay > 0) {
          Thread.sleep(iterationDelay * 1000);
        }
      } catch (Exception e) {
        log.error(e.getMessage());
        errors++;
        if (e instanceof NotifiableException) {
          // don't log exception
          log.error(
              " ✖ Cycle #"
                  + i
                  + " FAILED, retrying in "
                  + (SLEEP_LOOPWALLET_ON_ERROR / 1000)
                  + "s (total errors: "
                  + errors
                  + ", postmixIndex: "
                  + postmixWallet.getIndexHandler().get()
                  + ")");
        } else {
          // log exception
          log.error(
              " ✖ Cycle #"
                  + i
                  + " FAILED, retrying in "
                  + (SLEEP_LOOPWALLET_ON_ERROR / 1000)
                  + "s (total errors: "
                  + errors
                  + ", postmixIndex: "
                  + postmixWallet.getIndexHandler().get()
                  + ")",
              e);
        }
        Thread.sleep(SLEEP_LOOPWALLET_ON_ERROR);
      }
      i++;
    }
  }

  private boolean runLoopIteration(
      Bip84ApiWallet premixWallet,
      Pool pool,
      int nbClients,
      int clientDelay,
      boolean isAutoAggregatePostmix)
      throws Exception {
    // find mustMixUtxos
    List<UnspentResponse.UnspentOutput> mustMixUtxosUnique =
        fetchMustMixUtxosUnique(premixWallet, pool);
    List<UnspentResponse.UnspentOutput> liquidityUtxos = new ArrayList<>(); // TODO
    int missingMustMixUtxos =
        computeMissingMustMixUtxos(nbClients, mustMixUtxosUnique, liquidityUtxos);

    // do we have enough mustMixUtxo?
    while (missingMustMixUtxos > 0) {
      // not enough mustMixUtxos => Tx0
      for (int i = 0; i < missingMustMixUtxos; i++) {
        log.info(" • Tx0 (" + (i + 1) + "/" + missingMustMixUtxos + ")...");
        doRunTx0(pool, missingMustMixUtxos, isAutoAggregatePostmix);

        samouraiApi.refreshUtxos();
      }

      // refetch utxos
      samouraiApi.refreshUtxos();
      mustMixUtxosUnique = fetchMustMixUtxosUnique(premixWallet, pool);
      liquidityUtxos = new ArrayList<>(); // TODO
      missingMustMixUtxos =
          computeMissingMustMixUtxos(nbClients, mustMixUtxosUnique, liquidityUtxos);
    }
    log.info(" • New mix...");
    RunMixWallet runMixWallet =
        new RunMixWallet(whirlpoolClientConfig, torClientService, cliWalletService, bech32Util);
    return runMixWallet.runMix(mustMixUtxosUnique, pool, nbClients, clientDelay);
  }

  private List<UnspentResponse.UnspentOutput> fetchMustMixUtxosUnique(
      Bip84ApiWallet premixWallet, Pool pool) throws Exception {
    // fetch unspent utx0s
    log.info(" • Fetching unspent outputs from premix...");
    List<UnspentResponse.UnspentOutput> utxos = premixWallet.fetchUtxos();

    if (log.isDebugEnabled()) {
      log.debug("Found " + utxos.size() + " utxo from premix:");
      ClientUtils.logUtxos(utxos);
    }

    // find mustMixUtxos
    List<UnspentResponse.UnspentOutput> mustMixUtxosUnique =
        CliUtils.filterUtxoUniqueHash(CliUtils.filterUtxoMustMix(pool, utxos));
    if (log.isDebugEnabled()) {
      log.debug("Found " + mustMixUtxosUnique.size() + " unique mustMixUtxo");
    }
    return mustMixUtxosUnique;
  }

  private void doRunTx0(Pool pool, int missingMustMixUtxos, boolean isAutoAggregatePostmix)
      throws Exception {
    boolean success = false;
    while (!success) {
      try {
        CliWallet cliWallet = cliWalletService.getCliWallet();
        WhirlpoolUtxo spendFrom = cliWallet.findUtxoDepositForTx0(pool, OUTPUTS_PER_TX0, 1);
        cliWallet.tx0(pool, OUTPUTS_PER_TX0, spendFrom);
        success = true;
      } catch (EmptyWalletException e) {
        // deposit is empty => autoRefill when possible

        long requiredBalance =
            tx0Service.computeSpendFromBalanceMin(pool, samouraiApi.fetchFees(), OUTPUTS_PER_TX0)
                * missingMustMixUtxos;
        autoRefill(requiredBalance, isAutoAggregatePostmix);
      }
    }
  }

  private void autoRefill(long requiredBalance, boolean isAutoAggregatePostmix) throws Exception {
    Bip84ApiWallet depositWallet = cliWalletService.getCliWallet().getDepositWallet();
    Bip84ApiWallet premixWallet = cliWalletService.getCliWallet().getPremixWallet();
    Bip84ApiWallet postmixWallet = cliWalletService.getCliWallet().getPostmixWallet();

    // check total balance
    long depositBalance = depositWallet.fetchBalance();
    long premixBalance = premixWallet.fetchBalance();
    long postmixBalance = postmixWallet.fetchBalance();
    long totalBalance = depositBalance + premixBalance + postmixBalance;
    if (log.isDebugEnabled()) {
      log.debug("depositBalance=" + depositBalance);
      log.debug("premixBalance=" + premixBalance);
      log.debug("postmixBalance=" + postmixBalance);
      log.debug("totalBalance=" + totalBalance);
    }

    long missingBalance = totalBalance - requiredBalance;
    if (log.isDebugEnabled()) {
      log.debug("requiredBalance=" + requiredBalance + " => missingBalance=" + missingBalance);
    }
    if (totalBalance < requiredBalance) {
      throw new EmptyWalletException("Insufficient balance to continue", missingBalance);
    }

    String depositAddress =
        Bech32UtilGeneric.getInstance()
            .toBech32(
                depositWallet.getNextAddress(false), whirlpoolClientConfig.getNetworkParameters());
    String message =
        "Insufficient balance to continue. I need at least "
            + ClientUtils.satToBtc(missingBalance)
            + "btc to continue.\nPlease make a deposit to "
            + depositAddress;
    if (!isAutoAggregatePostmix) {
      CliUtils.waitUserAction(message);
      return;
    }

    // auto aggregate postmix
    log.info(" • depositWallet wallet is empty. Aggregating postmix to refill it...");
    boolean aggregateSuccess = walletAggregateService.consolidateTestnet();
    if (!aggregateSuccess) {
      CliUtils.waitUserAction(message);
    }

    samouraiApi.refreshUtxos();
  }

  private int computeMissingMustMixUtxos(
      int nbClients,
      List<UnspentResponse.UnspentOutput> mustMixUtxos,
      List<UnspentResponse.UnspentOutput> liquidityUtxos) {
    int missingAnonymitySet = nbClients - (mustMixUtxos.size() + liquidityUtxos.size());
    int missingMustMixUtxos = missingAnonymitySet > 0 ? missingAnonymitySet : 0;
    if (log.isDebugEnabled()) {
      log.debug(
          "Next mix needs "
              + nbClients
              + " utxos. I have "
              + mustMixUtxos.size()
              + " unique mustMixUtxo and "
              + liquidityUtxos.size()
              + " unique liquidityUtxo =>  "
              + missingMustMixUtxos
              + " more mustMixUtxo needed");
    }
    return missingMustMixUtxos;
  }
}
