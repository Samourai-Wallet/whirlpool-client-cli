package com.samourai.whirlpool.cli.utils;

import com.samourai.whirlpool.cli.beans.Encrypted;
import java.lang.invoke.MethodHandles;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Base64;

public class EncryptUtilsTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String SERIALIZED =
      "mD37gOLQe2BVzOe0OATJGF3oTcCzjkNplCBj/IxN8IucuRN55CL1Wolk6noorpFQGTJtUl4MB/W7WxypUSDeZZKAkCpqMe2VkajaONNKIfydkwWhrRFmL6J5iBI0sR92zqhWmIywfrCZjWNWz5I+yv/GizMeu/xZ8apAswftp6r+tSo=";
  private static final String IV = "mD37gOLQe2BVzOe0OATJGA==";
  private static final String SALT = "XehNwLOOQ2k=";
  private static final String CT =
      "lCBj/IxN8IucuRN55CL1Wolk6noorpFQGTJtUl4MB/W7WxypUSDeZZKAkCpqMe2VkajaONNKIfydkwWhrRFmL6J5iBI0sR92zqhWmIywfrCZjWNWz5I+yv/GizMeu/xZ8apAswftp6r+tSo=";

  @Test
  public void decrypt() throws Exception {
    String iv = IV;
    String salt = SALT;
    String ct = CT;
    Encrypted encrypted = new Encrypted(iv, salt, ct);
    String decryptedSeedWords = EncryptUtils.decrypt("test", encrypted);

    String expected =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon ability absorb acid";
    Assert.assertEquals(expected, decryptedSeedWords);
  }

  @Test
  public void serializeEncrypted() throws Exception {
    String iv = IV;
    String salt = SALT;
    String ct = CT;
    Encrypted encrypted = new Encrypted(iv, salt, ct);

    String serializeEncrypted = EncryptUtils.serializeEncrypted(encrypted);

    Assert.assertEquals(SERIALIZED, serializeEncrypted);

    Encrypted encryptedBis = EncryptUtils.unserializeEncrypted(serializeEncrypted);
    Assert.assertArrayEquals(encrypted.getIv(), encryptedBis.getIv());
    Assert.assertArrayEquals(encrypted.getSalt(), encryptedBis.getSalt());
    Assert.assertArrayEquals(encrypted.getCt(), encryptedBis.getCt());
  }

  @Test
  public void unserializeEncrypted() throws Exception {
    Encrypted encrypted = EncryptUtils.unserializeEncrypted(SERIALIZED);

    // expected
    byte[] iv = Base64.decode(IV);
    byte[] salt = Base64.decode(SALT);
    byte[] ct = Base64.decode(CT);

    Assert.assertArrayEquals(iv, encrypted.getIv());
    Assert.assertArrayEquals(salt, encrypted.getSalt());
    Assert.assertArrayEquals(ct, encrypted.getCt());

    String serializedBis = EncryptUtils.serializeEncrypted(encrypted);
    Assert.assertEquals(SERIALIZED, serializedBis);
  }
}
