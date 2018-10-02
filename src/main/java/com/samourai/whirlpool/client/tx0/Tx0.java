package com.samourai.whirlpool.client.tx0;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;

import java.util.HashMap;
import java.util.Map;

public class Tx0 {
    private Transaction tx;
    private Map<String, ECKey> toKeys = new HashMap<>();
    private Map<String, String> toUTXO = new HashMap<>();

    public Map<String, String> getToUTXO() {
        return toUTXO;
    }

    public Map<String, ECKey> getToKeys() {
        return toKeys;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }
}
