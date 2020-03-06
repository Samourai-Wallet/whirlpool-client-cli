package com.samourai.whirlpool.cli.exception;

import com.samourai.whirlpool.client.exception.NotifiableException;

public class AuthenticationException extends NotifiableException {

  public AuthenticationException(String error) {
    super(error);
  }
}
