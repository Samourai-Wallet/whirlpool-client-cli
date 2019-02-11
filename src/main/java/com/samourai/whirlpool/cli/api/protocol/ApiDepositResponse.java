package com.samourai.whirlpool.cli.api.protocol;

public class ApiDepositResponse {
  private String depositAddress;

  public ApiDepositResponse(String depositAddress) {
    this.depositAddress = depositAddress;
  }

  public String getDepositAddress() {
    return depositAddress;
  }
}
