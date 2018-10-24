package com.samourai.whirlpool.client;

import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.util.Assert;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

/**
 * Parsing command-line client arguments.
 */
public class ApplicationArgs {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ARG_DEBUG = "debug";
    private static final String ARG_NETWORK_ID = "network";
    private static final String ARG_UTXO = "utxo";
    private static final String ARG_UTXO_KEY = "utxo-key";
    private static final String ARG_UTXO_BALANCE = "utxo-balance";
    private static final String ARG_SEED_PASSPHRASE = "seed-passphrase";
    private static final String ARG_SEED_WORDS = "seed-words";
    private static final String ARG_PAYNYM_INDEX = "paynym-index";
    private static final String ARG_SERVER = "server";
    private static final String ARG_MIXS = "mixs";
    private static final String ARG_POOL_ID = "pool";
    private static final String ARG_TESTMODE = "test-mode";
    private static final String ARG_VPUB = "vpub";
    private static final String ARG_TX0 = "tx0";
    private static final String ARG_RPC_CLIENT_URL = "rpc-client-url";
    public static final String USAGE = "--network={main,test} [--utxo= --utxo-key= --utxo-balance=] or [--vpub= --rpc-client-url] --seed-passphrase= --seed-words= [--paynym-index=0] [--mixs=1] [--pool=] [--test-mode] [--server=host:port] [--debug] [--tx0]";
    private static final String UTXO_SEPARATOR = "-";

    private ApplicationArguments args;


    public ApplicationArgs(ApplicationArguments args) {
        this.args = args;
    }

    public boolean isDebug() {
        return args.containsOption(ARG_DEBUG);
    }

    public String getServer() {
        String server = requireOption(ARG_SERVER, "127.0.0.1:8080");
        Assert.notNull(server, "server is null");
        return server;
    }

    public NetworkParameters getNetworkParameters() {
        String networkId = requireOption(ARG_NETWORK_ID);
        NetworkParameters params = NetworkParameters.fromPmtProtocolID(networkId);
        Assert.notNull(params, "unknown network");
        return params;
    }

    public String getPoolId() {
        return optionalOption(ARG_POOL_ID);
    }

    private String getUtxo() {
        String utxo = requireOption(ARG_UTXO);
        Assert.notNull(utxo, "utxo is null");
        return utxo;
    }

    public String getUtxoHash() {
        String utxo = getUtxo();
        String utxoSplit[] = utxo.split(UTXO_SEPARATOR);
        String utxoHash = utxoSplit[0];
        return utxoHash;
    }

    public long getUtxoIdx() {
        String utxo = getUtxo();
        String utxoSplit[] = utxo.split(UTXO_SEPARATOR);
        long utxoIdx = Long.parseLong(utxoSplit[1]);
        return utxoIdx;
    }

    public String getUtxoKey() {
        String utxoKey = requireOption(ARG_UTXO_KEY);
        Assert.notNull(utxoKey, "utxoKey is null");
        return utxoKey;
    }

    public long getUtxoBalance() {
        long utxoBalance;
        try {
            utxoBalance = Integer.parseInt(requireOption(ARG_UTXO_BALANCE));
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Numeric value expected for option: "+ ARG_UTXO_BALANCE);
        }
        return utxoBalance;
    }

    public String getSeedWords() {
        String seedWords = requireOption(ARG_SEED_WORDS);
        Assert.notNull(seedWords, "seedWords are null");
        return seedWords;
    }

    public String getSeedPassphrase() {
        String seedPassphrase = requireOption(ARG_SEED_PASSPHRASE);
        Assert.notNull(seedPassphrase, "seedPassphrase is null");
        return seedPassphrase;
    }

    public int getPaynymIndex() {
        int paynymIndex = 0;
        try {
            String paynymIndexStr = optionalOption(ARG_PAYNYM_INDEX);
            if (paynymIndexStr != null) {
                paynymIndex = Integer.parseInt(paynymIndexStr);
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Numeric value expected for option: "+ ARG_PAYNYM_INDEX);
        }
        return paynymIndex;
    }

    public int getMixs() {
        final int mixs;
        try {
            mixs = Integer.parseInt(requireOption(ARG_MIXS, "1"));
            Assert.isTrue(mixs > 0, "mixs should be > 0");
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Numeric value expected for option: "+ ARG_MIXS);
        }
        return mixs;
    }

    public boolean isTestMode() {
        return args.containsOption(ARG_TESTMODE);
    }

    public Optional<Integer> getTx0() {
        if (!args.containsOption(ARG_TX0)) {
            return Optional.empty();
        }
        final int tx0;
        try {
            tx0 = Integer.parseInt(requireOption(ARG_TX0));
            Assert.isTrue(tx0 > 0, "tx0 should be > 0");
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Numeric value expected for option: "+ ARG_TX0);
        }
        return Optional.of(tx0);
    }

    public String getVPub() {
        return optionalOption(ARG_VPUB);
    }

    public String getRpcClientUrl() {
        return optionalOption(ARG_RPC_CLIENT_URL);
    }

    private String optionalOption(String name) {
        if (!args.getOptionNames().contains(name)) {
            return null;
        }
        return args.getOptionValues(name).iterator().next();
    }

    private String requireOption(String name) {
        if (!args.getOptionNames().contains(name) || !args.getOptionValues(name).iterator().hasNext()) {
            throw new IllegalArgumentException("Missing required option: "+name);
        }
        return args.getOptionValues(name).iterator().next();
    }

    private String requireOption(String name, String defaultValue) {
        if (!args.getOptionNames().contains(name) || !args.getOptionValues(name).iterator().hasNext()) {
            if (log.isDebugEnabled()) {
                log.debug("--" + name + "=" + defaultValue + " (default value)");
            }
            return defaultValue;
        }
        return args.getOptionValues(name).iterator().next();
    }
}
