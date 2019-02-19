package com.samourai.whirlpool.cli.api.protocol;

public class CliApiEndpoint {
  public static final String REST_PREFIX = "/rest/";

  public static final String REST_MIX = REST_PREFIX + "mix";
  public static final String REST_MIX_START = REST_PREFIX + "mix/start";
  public static final String REST_MIX_STOP = REST_PREFIX + "mix/stop";
  public static final String REST_TX0_CREATE = REST_PREFIX + "tx0/create";
  public static final String REST_TX0_POOLS = REST_PREFIX + "tx0/pools";
  public static final String REST_WALLET_UTXOS = REST_PREFIX + "wallet/utxos";
  public static final String REST_WALLET_DEPOSIT = REST_PREFIX + "wallet/deposit";

  public static final String[] REST_ENDPOINTS =
      new String[] {
        REST_MIX,
        REST_MIX_START,
        REST_MIX_STOP,
        REST_TX0_CREATE,
        REST_TX0_POOLS,
        REST_WALLET_UTXOS,
        REST_WALLET_DEPOSIT
      };
}
