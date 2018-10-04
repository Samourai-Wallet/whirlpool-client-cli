package com.samourai.whirlpool.client.run;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.ApplicationArgs;
import com.samourai.whirlpool.client.CliUtils;
import com.samourai.whirlpool.client.run.vpub.HdWalletFactory;
import com.samourai.whirlpool.client.run.vpub.MultiAddrResponse;
import com.samourai.whirlpool.client.run.vpub.UnspentResponse;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class RunVPub {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MIN_MUST_MIX = 3;

    public static final int ACCOUNT_DEPOSIT_AND_PREMIX = 0;
    public static final int CHAIN_DEPOSIT_AND_PREMIX = 0; // avec change_index

    public static final int ACCOUNT_POSTMIX = Integer.MAX_VALUE;
    public static final int CHAIN_POSTMIX = 0; // avec account_index

    //public static final int ACCOUNT_PREMIX = Integer.MAX_VALUE - 1;
    //public static final int CHAIN_PREMIX = 0;

    private static final long MINER_FEE_PER_MUSTMIX = 350;

    private static final int SLEEP_LOOPS_SECONDS = 20;

    private WhirlpoolClientConfig config;
    private NetworkParameters params;
    private HdWalletFactory hdWalletFactory;
    private SamouraiApi samouraiApi;

    private VpubWallet vpubWallet;

    public RunVPub(WhirlpoolClientConfig config) {
        this.config = config;
        this.params = config.getNetworkParameters();
        this.hdWalletFactory = new HdWalletFactory(params, CliUtils.computeMnemonicCode());
        this.samouraiApi = new SamouraiApi(config.getHttpClient());
    }

    public void run(Pool pool, ApplicationArgs appArgs) throws Exception {
        vpubWallet = computeVpubWallet(appArgs.getSeedPassphrase(), appArgs.getSeedWords(), appArgs.getVPub());

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

            // fetch spend address info
            log.info(" • Fetching addresses for VPub...");
            MultiAddrResponse.Address address = vpubWallet.fetchAddress(samouraiApi);

            // tx0
            log.info(" • Tx0...");
            long destinationValue = WhirlpoolProtocol.computeInputBalanceMin(pool.getDenomination(), false, MINER_FEE_PER_MUSTMIX);
            new RunTx0VPub(params).runTx0(utxos, address, vpubWallet, destinationValue);
        } else {
            log.info(" • New mix...");
            new RunMixVPub(config).runMix(mustMixUtxos, vpubWallet, pool, samouraiApi);
        }
    }

    private VpubWallet computeVpubWallet(String passphrase, String seedWords, String vpub) throws Exception {
        MnemonicCode mc = CliUtils.computeMnemonicCode();
        HD_Wallet bip44w = hdWalletFactory.restoreWallet(seedWords, passphrase, 1);
        BIP47Wallet bip47w = new BIP47Wallet(47, mc, params, Hex.decode(bip44w.getSeedHex()), bip44w.getPassphrase(), 1);
        HD_Wallet bip84w = new HD_Wallet(84, mc, params, Hex.decode(bip44w.getSeedHex()), bip44w.getPassphrase(), 1);
        return new VpubWallet(bip44w, bip47w, bip84w, vpub);
    }
}
