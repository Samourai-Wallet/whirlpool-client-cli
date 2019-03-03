package com.samourai.whirlpool.cli.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.Console;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static String generateUniqueString() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public static void waitUserAction(String message) throws NotifiableException {
    Console console = System.console();
    if (console != null) {
      log.info("⣿ ACTION REQUIRED ⣿ " + message);
      log.info("Press <ENTER> when ready:");
      console.readLine();
    } else {
      throw new NotifiableException("⣿ ACTION REQUIRED ⣿ " + message);
    }
  }

  public static String readUserInput(String message, boolean secret) throws NotifiableException {
    Console console = System.console();
    if (console != null) {
      console.printf("⣿ INPUT REQUIRED ⣿ " + message + "?>");
      String line = secret ? new String(console.readPassword()).trim() : console.readLine().trim();
      if (line.isEmpty()) {
        return null;
      }
      return line;
    } else {
      throw new NotifiableException("⣿ INPUT REQUIRED ⣿ " + message + "?>");
    }
  }

  public static String sha256Hash(String str) {
    return Sha256Hash.wrap(Sha256Hash.hash(str.getBytes())).toString();
  }

  public static String encryptSeedWords(String key, String seedWords) throws Exception {
    /*SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(key, salt, 1024, 256);
    SecretKey tmp = factory.generateSecret(spec);
    SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
    String plaintext = new String(cipher.doFinal(ciphertext), "UTF-8");
    return plaintext;*/
    return null;
  }

  public static String decryptSeedWords(String key, String encryptedSeedWords64) throws Exception {
    String encryptedSeedWords = new String(Base64.decode(encryptedSeedWords64), "UTF-8");
    System.err.println("****encryptedSeedWords=" + encryptedSeedWords);
    JsonNode encryptedJson = new ObjectMapper().readTree(encryptedSeedWords);

    /*SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(key, salt, 1024, 256);
    SecretKey tmp = factory.generateSecret(spec);
    SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
    String plaintext = new String(cipher.doFinal(ciphertext), "UTF-8");
    return plaintext;*/
    return encryptedSeedWords;
  }
}
