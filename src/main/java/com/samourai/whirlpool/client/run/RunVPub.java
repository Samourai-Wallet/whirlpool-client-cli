package com.samourai.whirlpool.client.run;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
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
import java.util.stream.Collectors;

public class RunVPub {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MIN_MUST_MIX = 3;

    public static final int ACCOUNT_DEPOSIT = 0;
    public static final int CHAIN_DEPOSIT = 0;

    public static final int ACCOUNT_PREMIX = Integer.MAX_VALUE - 1;
    public static final int CHAIN_PREMIX = 0;

    private static final long MINER_FEE_PER_MUSTMIX = 300;

    private WhirlpoolClientConfig config;
    private NetworkParameters params;
    private HdWalletFactory hdWalletFactory;
    private SamouraiApi samouraiApi;

    private HD_Wallet bip44w;
    private BIP47Wallet bip47w;
    private HD_Wallet bip84w;

    public RunVPub(WhirlpoolClientConfig config) {
        this.config = config;
        this.params = config.getNetworkParameters();
        this.hdWalletFactory = new HdWalletFactory(params, CliUtils.computeMnemonicCode());
        this.samouraiApi = new SamouraiApi(config.getHttpClient());
    }

    public void run(Pool pool, String seedWords, String passphrase, int paynymIndex, String vpub) throws Exception {
        initWallets(seedWords, passphrase);

        // fetch unspent utx0s
        log.info(" • Fetching unspent outputs for VPub...");
        List<UnspentResponse.UnspentOutput> utxos = samouraiApi.fetchUtxos(vpub);
        if (!utxos.isEmpty()) {
            log.info("Found " + utxos.size() + " utxo from VPub:");
            CliUtils.printUtxos(utxos);
        } else {
            log.error("ERROR: No utxo available from VPub.");
            return;
        }

        // find mustMixUtxos
        long balanceMin = WhirlpoolProtocol.computeInputBalanceMin(pool.getDenomination(), false, pool.getMinerFeeMin());
        long balanceMax = WhirlpoolProtocol.computeInputBalanceMax(pool.getDenomination(), false, pool.getMinerFeeMax());
        List<UnspentResponse.UnspentOutput> mustMixUtxos = utxos.stream().filter(utxo -> utxo.value >= balanceMin && utxo.value <= balanceMax).collect(Collectors.toList());
        log.info("Found " + mustMixUtxos.size() + " mustMixUtxo (" + balanceMin + " < mustMixUtxo < " + balanceMax + ")");

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
            MultiAddrResponse.Address address = samouraiApi.findAddress(vpub);
            if (address == null) {
                throw new Exception("Address not found");
            }

            // tx0
            log.info(" • Tx0...");
            long destinationValue = WhirlpoolProtocol.computeInputBalanceMin(pool.getDenomination(), false, MINER_FEE_PER_MUSTMIX);
            new RunTx0VPub(params).runTx0(utxos, address, bip84w, destinationValue);
        } else {
            log.info(" • New mix...");
            paynymIndex = new RunMixVPub(config).runMix(mustMixUtxos, bip47w, bip84w, pool, paynymIndex);
            log.info("=> paynymIndex=" + paynymIndex);
        }
        // TODO LOOP
    }

    private void initWallets(String seedWords, String passphrase) throws Exception {
        MnemonicCode mc = CliUtils.computeMnemonicCode();
        bip44w = hdWalletFactory.restoreWallet(seedWords, passphrase, 1);
        bip47w = new BIP47Wallet(47, mc, params, Hex.decode(bip44w.getSeedHex()), bip44w.getPassphrase(), 1);
        bip84w = new HD_Wallet(84, mc, params, Hex.decode(bip44w.getSeedHex()), bip44w.getPassphrase(), 1);
    }
}
