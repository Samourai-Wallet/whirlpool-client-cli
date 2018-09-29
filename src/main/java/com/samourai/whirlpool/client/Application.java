package com.samourai.whirlpool.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.impl.Bip47Util;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.mix.MixParams;
import com.samourai.whirlpool.client.mix.handler.IMixHandler;
import com.samourai.whirlpool.client.mix.handler.MixHandler;
import com.samourai.whirlpool.client.mix.listener.MixStep;
import com.samourai.whirlpool.client.mix.listener.MixSuccess;
import com.samourai.whirlpool.client.utils.LogbackUtils;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import com.samourai.whirlpool.client.whirlpool.listener.LoggingWhirlpoolClientListener;
import com.samourai.whirlpool.client.whirlpool.listener.WhirlpoolClientListener;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Command-line client.
 */
@EnableAutoConfiguration
public class Application implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

    private ApplicationArgs appArgs;
    private boolean done = false;


    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        this.appArgs = new ApplicationArgs(args);

        // enable debug logs with --debug
        if (appArgs.isDebug()) {
            LogbackUtils.setLogLevel("com.samourai.whirlpool.client", Level.DEBUG.toString());
        }

        log.info("------------ whirlpool-client ------------");
        log.info("Running whirlpool-client {}", Arrays.toString(args.getSourceArgs()));
        try {
            String server = appArgs.getServer();
            NetworkParameters params = appArgs.getNetworkParameters();
            new Context(params); // initialize bitcoinj context

            // instanciate client
            IHttpClient httpClient = new JavaHttpClient();
            IStompClient stompClient = new JavaStompClient();
            WhirlpoolClientConfig config = new WhirlpoolClientConfig(httpClient, stompClient, server, params);
            if (appArgs.isTestMode()) {
                config.setTestMode(true);
                if (log.isDebugEnabled()) {
                    log.debug("--test-mode: tx0 verifications will be skiped (if server allows it)");
                }
            }
            WhirlpoolClient whirlpoolClient = WhirlpoolClientImpl.newClient(config);

            // fetch pools
            try {
                log.info(" • Retrieving pools...");
                Pools pools = whirlpoolClient.fetchPools();

                String poolId = appArgs.getPoolId();
                if (poolId != null) {
                    // if --pool is provided, find pool
                   Pool pool = pools.findPoolById(poolId);
                   if (pool != null) {
                       // pool found, go whirlpool
                       try {
                           new RunWhirlpool().run(appArgs, whirlpoolClient, pool.getPoolId(), pool.getDenomination());
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                       return;
                   } else {
                       log.error("Pool not found: " + poolId);
                   }
                }

                // show pools list if --pool is not provided/found
                printPools(pools);
            } catch(Exception e) {
                log.error("", e);
            }
        }
        catch(IllegalArgumentException e) {
            log.info("Invalid arguments: "+e.getMessage());
            log.info("Usage: whirlpool-client "+ApplicationArgs.USAGE);
        }
    }

    // show available pools
    private void printPools(Pools pools) {
        try {
            String lineFormat = "| %15s | %6s | %15s | %22s | %12s | %15s | %13s |\n";
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(lineFormat, "POOL ID", "DENOM.", "STATUS", "USERS", "ELAPSED TIME", "ANONYMITY SET", "MINER FEE"));
            sb.append(String.format(lineFormat, "", "(btc)", "", "(confirmed/registered)", "", "(target/min)", "min-max (sat)"));
            for (Pool pool : pools.getPools()) {
                sb.append(String.format(lineFormat, pool.getPoolId(),  satToBtc(pool.getDenomination()), pool.getMixStatus(), pool.getMixNbConfirmed() + " / " + pool.getNbRegistered(), pool.getElapsedTime()/1000 + "s", pool.getMixAnonymitySet() + " / " + pool.getMinAnonymitySet(), pool.getMinerFeeMin() + " - " + pool.getMinerFeeMax()));
            }
            log.info("\n" + sb.toString());
            log.info("Tip: use --pool argument to select a pool");
        } catch(Exception e) {
            log.error("", e);
        }
    }

    private double satToBtc(long sat) {
        return sat / 100000000.0;
    }
}
