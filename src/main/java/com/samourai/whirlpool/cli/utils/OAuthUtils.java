package com.samourai.whirlpool.cli.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static boolean validate(DecodedJWT token) {
    // check expiration
    boolean valid = token.getExpiresAt().after(new Date());
    return valid;
  }

  public static DecodedJWT decode(String token) {
    return JWT.decode(token);
  }
}
