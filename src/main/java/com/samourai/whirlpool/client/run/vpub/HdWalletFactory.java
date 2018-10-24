package com.samourai.whirlpool.client.run.vpub;

import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.util.FormatsUtilGeneric;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

public class HdWalletFactory {
  private NetworkParameters params;
  private MnemonicCode mc;

  public HdWalletFactory(NetworkParameters params, MnemonicCode mc) {
    this.params = params;
    this.mc = mc;
  }

  public HD_Wallet restoreWallet(String data, String passphrase, int nbAccounts)
      throws AddressFormatException, IOException, DecoderException,
          MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException,
          MnemonicException.MnemonicChecksumException {

    HD_Wallet hdw = null;

    if (passphrase == null) {
      passphrase = "";
    }

    if (mc != null) {
      List<String> words = null;

      byte[] seed = null;
      if (data.matches(FormatsUtilGeneric.XPUB)) {
        String[] xpub = data.split(":");
        hdw = new HD_Wallet(params, xpub);
      } else if (data.matches(FormatsUtilGeneric.HEX) && data.length() % 4 == 0) {
        seed = Hex.decodeHex(data.toCharArray());
        hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
      } else {
        data = data.toLowerCase().replaceAll("[^a-z]+", " "); // only use for BIP39 English
        words = Arrays.asList(data.trim().split("\\s+"));
        seed = mc.toEntropy(words);
        hdw = new HD_Wallet(44, mc, params, seed, passphrase, nbAccounts);
      }
    }
    return hdw;
  }
}
