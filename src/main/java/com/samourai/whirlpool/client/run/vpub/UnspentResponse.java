package com.samourai.whirlpool.client.run.vpub;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bouncycastle.util.encoders.Hex;

public class UnspentResponse {
    public UnspentOutput[] unspent_outputs;

    public UnspentResponse() {
    }

    public static class UnspentOutput {
        private static final String PATH_SEPARATOR = "/";
        public String tx_hash;
        public int tx_output_n;
        public long value;
        public String script;
        public String addr;
        public int confirmations;
        public Xpub xpub;

        public int computePathChainIndex() {
            return Integer.parseInt(xpub.path.split(PATH_SEPARATOR)[1]);
        }

        public int computePathAddressIndex() {
            return Integer.parseInt(xpub.path.split(PATH_SEPARATOR)[2]);
        }

        public TransactionOutPoint computeOutpoint(NetworkParameters params) {
            Sha256Hash sha256Hash = Sha256Hash.wrap(Hex.decode(tx_hash));
            return new TransactionOutPoint(params, tx_output_n, sha256Hash, Coin.valueOf(value));
        }

        public static class Xpub {
            public String path;
        }
    }

}
