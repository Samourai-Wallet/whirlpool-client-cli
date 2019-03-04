package com.samourai.whirlpool.cli.beans;

import org.bouncycastle.util.encoders.Base64;

public class Encrypted {
  private byte[] iv;
  private byte[] salt;
  private byte[] ct;

  public Encrypted(byte[] iv, byte[] salt, byte[] ct) {
    this.iv = iv;
    this.salt = salt;
    this.ct = ct;
  }

  public Encrypted(String iv, String salt, String ct) {
    this(Base64.decode(iv), Base64.decode(salt), Base64.decode(ct));
  }

  public byte[] getIv() {
    return iv;
  }

  public byte[] getSalt() {
    return salt;
  }

  public byte[] getCt() {
    return ct;
  }
}
