package com.samourai.whirlpool.client.run;

import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.run.vpub.UnspentResponse;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class RunVPubLoop {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MIN_MUST_MIX = 3;

    public static final int ACCOUNT_DEPOSIT_AND_PREMIX = 0;
    public static final int CHAIN_DEPOSIT_AND_PREMIX = 0; // with change_index

    public static final int ACCOUNT_POSTMIX = Integer.MAX_VALUE;
    public static final int CHAIN_POSTMIX = 0; // with account_index

    public static final long MINER_FEE_PER_MUSTMIX = 450;

    private static final int SLEEP_LOOPS_SECONDS = 20;

    private WhirlpoolClientConfig config;
    private NetworkParameters params;
    private SamouraiApi samouraiApi;

    private VpubWallet vpubWallet;

    public RunVPubLoop(WhirlpoolClientConfig config, SamouraiApi samouraiApi) {
        this.config = config;
        this.params = config.getNetworkParameters();
        this.samouraiApi = samouraiApi;
    }

    public void run(Pool pool, VpubWallet vpubWallet) throws Exception {
        this.vpubWallet = vpubWallet;

        while(true) {
            log.info(" --------------------------------------- ");
            runLoop(pool);

            log.info(" => Next loop in " +  SLEEP_LOOPS_SECONDS + " seconds...");
            Thread.sleep(SLEEP_LOOPS_SECONDS*1000);
        }
    }

    public void runLoop(Pool pool) throws Exception {
        // fetch unspent utx0s
        log.info(" • Fetching unspent outputs from premix...");
        List<UnspentResponse.UnspentOutput> utxos = vpubWallet.fetchUtxos(samouraiApi);
        if (!utxos.isEmpty()) {
            log.info("Found " + utxos.size() + " utxo from premix:");
            CliUtils.printUtxos(utxos);
        } else {
            log.error("ERROR: No utxo available from VPub.");
            return;
        }

        // find mustMixUtxos
        List<UnspentResponse.UnspentOutput> mustMixUtxos = CliUtils.filterUtxoMustMix(pool, utxos);
        log.info("Found " + mustMixUtxos.size() + " mustMixUtxo");

        // find liquidityUtxos
        List<UnspentResponse.UnspentOutput> liquidityUtxos = new ArrayList<>(); // TODO

        // how many utxos do we need for mix?
        int missingMustmixs = MIN_MUST_MIX - mustMixUtxos.size();
        int missingAnonymitySet = pool.getMixAnonymitySet() - (mustMixUtxos.size() + liquidityUtxos.size());
        log.info("Next mix needs " + pool.getMixAnonymitySet() + " utxos (minMustMix=" + MIN_MUST_MIX + " mustMix). I have " + mustMixUtxos.size() + " mustMixUtxo and " + liquidityUtxos.size() + " liquidityUtxo.");

        // do we have enough mustMixUtxo?
        int missingMustMixUtxos = Math.max(missingMustmixs, missingAnonymitySet);
        if (missingMustMixUtxos > 0) {
            // not enough mustMixUtxos => new Tx0
            log.info(" => I need " + missingMustMixUtxos + " more mustMixUtxo. Preparing new Tx0.");

            // tx0
            log.info(" • Tx0...");
            new RunTx0VPub(params, samouraiApi).runTx0(utxos, vpubWallet, pool);
        } else {
            log.info(" • New mix...");
            new RunMixVPub(config).runMix(mustMixUtxos, vpubWallet, pool, samouraiApi);
        }
    }
}
