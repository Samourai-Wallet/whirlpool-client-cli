package com.samourai.whirlpool.client;

import com.samourai.http.client.IHttpClient;
import com.samourai.http.client.JavaHttpClient;
import com.samourai.stomp.client.IStompClient;
import com.samourai.stomp.client.JavaStompClient;
import com.samourai.whirlpool.client.utils.LogbackUtils;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientConfig;
import com.samourai.whirlpool.client.whirlpool.WhirlpoolClientImpl;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.client.whirlpool.beans.Pools;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.Arrays;

/**
 * Command-line client.
 */
@EnableAutoConfiguration
public class Application implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private ApplicationArgs appArgs;

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
            NetworkParameters params = appArgs.getNetworkParameters();
            new Context(params); // initialize bitcoinj context

            // instanciate client
            String server = appArgs.getServer();
            WhirlpoolClient whirlpoolClient = initWhirlpoolClient(server, params);

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
                       new RunWhirlpool().run(appArgs, whirlpoolClient, pool.getPoolId(), pool.getDenomination());
                       return;
                   } else {
                       log.error("Pool not found: " + poolId);
                   }
                }

                // show pools list if --pool is not provided/found
                new RunListPools().run(pools);
                log.info("Tip: use --pool argument to select a pool");
            } catch(Exception e) {
                log.error("", e);
            }
        }
        catch(IllegalArgumentException e) {
            log.info("Invalid arguments: "+e.getMessage());
            log.info("Usage: whirlpool-client "+ApplicationArgs.USAGE);
        }
    }

    private WhirlpoolClient initWhirlpoolClient(String server, NetworkParameters params) {
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
        return whirlpoolClient;
    }
}
