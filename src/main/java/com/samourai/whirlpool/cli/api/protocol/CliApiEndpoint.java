package com.samourai.whirlpool.cli.api.protocol;

public class CliApiEndpoint {
  public static final String REST_PREFIX = "/rest/";

  public static final String REST_POOLS = REST_PREFIX + "pools";

  public static final String REST_WALLET_DEPOSIT = REST_PREFIX + "wallet/deposit";

  public static final String REST_MIX = REST_PREFIX + "mix";
  public static final String REST_MIX_START = REST_PREFIX + "mix/start";
  public static final String REST_MIX_STOP = REST_PREFIX + "mix/stop";

  public static final String REST_UTXOS = REST_PREFIX + "utxos";
  public static final String REST_UTXO_CONFIGURE = REST_PREFIX + "utxos/{hash}:{index}";
  public static final String REST_UTXO_TX0 = REST_PREFIX + "utxos/{hash}:{index}/tx0";
  public static final String REST_UTXO_STARTMIX = REST_PREFIX + "utxos/{hash}:{index}/startMix";
  public static final String REST_UTXO_STOPMIX = REST_PREFIX + "utxos/{hash}:{index}/stopMix";

  public static final String[] REST_ENDPOINTS =
      new String[] {
        REST_POOLS,
        REST_WALLET_DEPOSIT,
        REST_MIX,
        REST_MIX_START,
        REST_MIX_STOP,
        REST_UTXOS,
        REST_UTXO_CONFIGURE,
        REST_UTXO_TX0,
        REST_UTXO_STARTMIX,
        REST_UTXO_STOPMIX
      };
}
