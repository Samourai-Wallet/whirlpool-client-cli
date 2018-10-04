package com.samourai.whirlpool.client;

import com.samourai.whirlpool.client.run.vpub.UnspentResponse;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import org.bitcoinj.crypto.MnemonicCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

public class CliUtils {
    private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

    private final static int DEPOSIT_ACCOUNT = 0;
    private final static int WHIRLPOOL_PREMIX_ACCOUNT = Integer.MAX_VALUE - 1;
    private final static int WHIRLPOOL_POSTMIX = Integer.MAX_VALUE;

    public static double satToBtc(long sat) {
        return sat / 100000000.0;
    }

    public static MnemonicCode computeMnemonicCode() {
        InputStream wis = CliUtils.class.getResourceAsStream("/en_US.txt");
        try {
            MnemonicCode mc = new MnemonicCode(wis, CliUtils.BIP39_ENGLISH_SHA256);
            return mc;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void printUtxos(List<UnspentResponse.UnspentOutput> utxos) {
        String lineFormat = "| %7s | %10s | %70s |\n";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(lineFormat, "BALANCE", "CONFIRMS", "UTXO"));
        sb.append(String.format(lineFormat, "(btc)", "", ""));
        for (UnspentResponse.UnspentOutput o : utxos) {
            String utxo = o.tx_hash + ":" + o.tx_output_n;
            sb.append(String.format(lineFormat, CliUtils.satToBtc(o.value), o.confirmations, utxo));
        }
        log.info("\n" + sb.toString());
    }

    public static List<UnspentResponse.UnspentOutput> filterUtxoMustMix(Pool pool, List<UnspentResponse.UnspentOutput> utxos) {
        long balanceMin = WhirlpoolProtocol.computeInputBalanceMin(pool.getDenomination(), false, pool.getMinerFeeMin());
        long balanceMax = WhirlpoolProtocol.computeInputBalanceMax(pool.getDenomination(), false, pool.getMinerFeeMax());
        List<UnspentResponse.UnspentOutput> mustMixUtxos = utxos.stream().filter(utxo -> utxo.value >= balanceMin && utxo.value <= balanceMax).collect(Collectors.toList());
        return mustMixUtxos;
    }

}
