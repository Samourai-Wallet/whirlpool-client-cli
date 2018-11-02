package com.samourai.whirlpool.client;

import com.samourai.api.SamouraiApi;
import com.samourai.api.beans.UnspentResponse;
import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.whirlpool.client.run.VpubWallet;
import com.samourai.whirlpool.client.run.vpub.HdWalletFactory;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String BIP39_ENGLISH_SHA256 =
      "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

  public static double satToBtc(long sat) {
    return sat / 100000000.0;
  }

  public static MnemonicCode computeMnemonicCode() {
    InputStream wis = CliUtils.class.getResourceAsStream("/en_US.txt");
    try {
      MnemonicCode mc = new MnemonicCode(wis, CliUtils.BIP39_ENGLISH_SHA256);
      return mc;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void printUtxos(List<UnspentResponse.UnspentOutput> utxos) {
    String lineFormat = "| %7s | %10s | %70s | %50s | %16s |\n";
    StringBuilder sb = new StringBuilder();
    sb.append(String.format(lineFormat, "BALANCE", "CONFIRMS", "UTXO", "ADDRESS", "PATH"));
    sb.append(String.format(lineFormat, "(btc)", "", "", "", ""));
    for (UnspentResponse.UnspentOutput o : utxos) {
      String utxo = o.tx_hash + ":" + o.tx_output_n;
      sb.append(
          String.format(
              lineFormat, CliUtils.satToBtc(o.value), o.confirmations, utxo, o.addr, o.getPath()));
    }
    log.info("\n" + sb.toString());
  }

  public static List<UnspentResponse.UnspentOutput> filterUtxoMustMix(
      Pool pool, List<UnspentResponse.UnspentOutput> utxos) {
    long balanceMin =
        WhirlpoolProtocol.computeInputBalanceMin(
            pool.getDenomination(), false, pool.getMinerFeeMin());
    long balanceMax =
        WhirlpoolProtocol.computeInputBalanceMax(
            pool.getDenomination(), false, pool.getMinerFeeMax());
    List<UnspentResponse.UnspentOutput> mustMixUtxos =
        utxos
            .stream()
            .filter(utxo -> utxo.value >= balanceMin && utxo.value <= balanceMax)
            .collect(Collectors.toList());
    return mustMixUtxos;
  }

  public static VpubWallet computeVpubWallet(
      String passphrase,
      String seedWords,
      NetworkParameters params,
      HdWalletFactory hdWalletFactory,
      SamouraiApi samouraiApi)
      throws Exception {
    MnemonicCode mc = CliUtils.computeMnemonicCode();
    HD_Wallet bip44w = hdWalletFactory.restoreWallet(seedWords, passphrase, 1);
    BIP47Wallet bip47w =
        new BIP47Wallet(47, mc, params, Hex.decode(bip44w.getSeedHex()), bip44w.getPassphrase(), 1);
    HD_Wallet bip84w =
        new HD_Wallet(84, mc, params, Hex.decode(bip44w.getSeedHex()), bip44w.getPassphrase(), 1);
    return new VpubWallet(bip44w, bip47w, bip84w, samouraiApi);
  }
}
