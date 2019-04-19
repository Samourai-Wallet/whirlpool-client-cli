package com.samourai.whirlpool.cli.utils;

import com.samourai.whirlpool.cli.beans.Encrypted;
import java.lang.invoke.MethodHandles;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int CHECK_IV_LENGTH = 16;
  private static final int CHECK_SALT_LENGTH = 8;

  private static final String CRYPT_SF_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final String CRYPT_ALGORITHM = "AES";
  private static final String CRYPT_CIPHER = "AES/GCM/NoPadding";
  private static final int CRYPT_KEY_LENGTH = 256;
  private static final int CRYPT_TAG_LENGTH = 128;
  private static final int CRYPT_ITERATIONS = 10000;

  private static final SecureRandom secureRandom = new SecureRandom();

  public static Encrypted encrypt(String key, String plain) throws Exception {
    Encrypted encrypted =
        encrypt(
            key.toCharArray(),
            plain.getBytes("UTF-8"),
            CRYPT_ITERATIONS,
            CRYPT_KEY_LENGTH,
            CRYPT_TAG_LENGTH,
            CHECK_IV_LENGTH,
            CHECK_SALT_LENGTH);
    return encrypted;
  }

  public static String decrypt(String key, String encryptedSerialized) throws Exception {
    Encrypted encrypted = unserializeEncrypted(encryptedSerialized);
    return decrypt(key, encrypted);
  }

  public static String decrypt(String key, Encrypted encrypted) throws Exception {
    String plaintext =
        decrypt(
            key.toCharArray(),
            encrypted.getIv(),
            encrypted.getSalt(),
            encrypted.getCt(),
            CRYPT_ITERATIONS,
            CRYPT_KEY_LENGTH,
            CRYPT_TAG_LENGTH);
    return plaintext;
  }

  public static Encrypted encrypt(
      char[] key,
      byte[] plainBytes,
      int iterations,
      int keyLength,
      int tagLength,
      int ivLength,
      int saltLength)
      throws Exception {

    final byte[] ivBytes = new byte[ivLength];
    secureRandom.nextBytes(ivBytes);

    final byte[] saltBytes = new byte[saltLength];
    secureRandom.nextBytes(saltBytes);

    SecretKeyFactory factory = SecretKeyFactory.getInstance(CRYPT_SF_ALGORITHM);
    KeySpec spec = new PBEKeySpec(key, saltBytes, iterations, keyLength);
    SecretKey tmp = factory.generateSecret(spec);
    SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), CRYPT_ALGORITHM);

    Cipher cipher = Cipher.getInstance(CRYPT_CIPHER);
    GCMParameterSpec ivSpec = new GCMParameterSpec(tagLength, ivBytes);
    cipher.init(Cipher.ENCRYPT_MODE, secret, ivSpec);

    byte[] ctBytes = cipher.doFinal(plainBytes);

    Encrypted encrypted = new Encrypted(ivBytes, saltBytes, ctBytes);
    return encrypted;
  }

  public static String decrypt(
      char[] key,
      byte[] ivBytes,
      byte[] saltBytes,
      byte[] ctBytes,
      int iterations,
      int keyLength,
      int tagLength)
      throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(CRYPT_SF_ALGORITHM);
    KeySpec spec = new PBEKeySpec(key, saltBytes, iterations, keyLength);
    SecretKey tmp = factory.generateSecret(spec);
    SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), CRYPT_ALGORITHM);

    Cipher cipher = Cipher.getInstance(CRYPT_CIPHER);
    GCMParameterSpec ivSpec = new GCMParameterSpec(tagLength, ivBytes);
    cipher.init(Cipher.DECRYPT_MODE, secret, ivSpec);

    String plaintext = new String(cipher.doFinal(ctBytes), "UTF-8");
    return plaintext;
  }

  public static String serializeEncrypted(Encrypted encrypted) throws Exception {
    byte[] iv = encrypted.getIv();
    byte[] salt = encrypted.getSalt();
    byte[] ct = encrypted.getCt();

    if (iv.length != CHECK_IV_LENGTH) {
      throw new Exception("Invalid IV length: " + iv.length + " vs " + CHECK_IV_LENGTH);
    }
    if (salt.length != CHECK_SALT_LENGTH) {
      throw new Exception("Invalid SALT length: " + salt.length + " vs " + CHECK_SALT_LENGTH);
    }

    byte[] concat = new byte[iv.length + salt.length + ct.length];
    // concat iv
    System.arraycopy(iv, 0, concat, 0, iv.length);
    // concat salt
    System.arraycopy(salt, 0, concat, iv.length, salt.length);
    // concat ct
    System.arraycopy(ct, 0, concat, iv.length + salt.length, ct.length);

    // base64 encode
    return org.bouncycastle.util.encoders.Base64.toBase64String(concat);
  }

  protected static Encrypted unserializeEncrypted(String apiEncryptedSerialized) throws Exception {
    // base64 decode
    byte[] concat = org.bouncycastle.util.encoders.Base64.decode(apiEncryptedSerialized);

    // un-concat
    if (concat.length <= CHECK_IV_LENGTH + CHECK_SALT_LENGTH) {
      throw new Exception(
          "Invalid concat length: "
              + concat.length
              + " <= "
              + (CHECK_IV_LENGTH + CHECK_SALT_LENGTH));
    }

    byte[] iv = Arrays.copyOfRange(concat, 0, CHECK_IV_LENGTH);
    byte[] salt = Arrays.copyOfRange(concat, CHECK_IV_LENGTH, CHECK_IV_LENGTH + CHECK_SALT_LENGTH);
    byte[] ct = Arrays.copyOfRange(concat, CHECK_IV_LENGTH + CHECK_SALT_LENGTH, concat.length);

    Encrypted encrypted = new Encrypted(iv, salt, ct);
    return encrypted;
  }
}
