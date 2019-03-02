package com.samourai.whirlpool.cli;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.util.Assert;

/** Parsing command-line client arguments. */
public class ApplicationArgs {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int LISTEN_DEFAULT_PORT = 8899;

  private static final String ARG_DEBUG = "debug";
  private static final String ARG_DEBUG_CLIENT = "debug-client";
  private static final String ARG_UTXO = "utxo";
  private static final String ARG_UTXO_KEY = "utxo-key";
  private static final String ARG_UTXO_BALANCE = "utxo-balance";
  private static final String ARG_SEED_PASSPHRASE = "seed-passphrase";
  private static final String ARG_SEED_WORDS = "seed-words";
  private static final String ARG_SERVER = "server";
  private static final String ARG_MIXS = "mixs";
  private static final String ARG_LIST_POOLS = "list-pools";
  private static final String ARG_POOL_ID = "pool";
  private static final String ARG_SCODE = "scode";
  private static final String ARG_CLIENTS = "clients";
  private static final String ARG_CLIENT_DELAY = "client-delay";
  private static final String ARG_TX0_DELAY = "tx0-delay";
  private static final String ARG_TX0_MAX_OUTPUTS = "tx0-max-outputs";
  private static final String ARG_AGGREGATE_POSTMIX = "aggregate-postmix";
  private static final String ARG_AUTO_AGGREGATE_POSTMIX = "auto-aggregate-postmix";
  private static final String ARG_AUTO_TX0 = "auto-tx0";
  private static final String ARG_AUTO_MIX = "auto-mix";
  private static final String ARG_PUSHTX = "pushtx";
  private static final String ARG_TOR = "tor";
  private static final String ARG_LISTEN = "listen";
  private static final String ARG_API_KEY = "api-key";
  private static final String UTXO_SEPARATOR = "-";

  private ApplicationArguments args;

  public ApplicationArgs(ApplicationArguments args) {
    this.args = args;
  }

  public void override(CliConfig cliConfig) {
    String value;
    Boolean valueBool;
    Integer valueInt;

    value = optionalOption(ARG_SERVER);
    if (value != null) {
      cliConfig.setServer(Enum.valueOf(WhirlpoolServer.class, value));
    }

    Collection<String> poolIdsBypriority = getPoolIdsByPriority();
    if (!poolIdsBypriority.isEmpty()) {
      cliConfig.getMix().setPoolIdsByPriority(poolIdsBypriority);
    }

    value = optionalOption(ARG_SCODE);
    if (value != null) {
      cliConfig.setScode(value);
    }

    valueBool = optionalBoolean(ARG_AUTO_TX0);
    if (valueBool != null) {
      cliConfig.getMix().setAutoTx0(valueBool);
    }

    valueBool = optionalBoolean(ARG_AUTO_MIX);
    if (valueBool != null) {
      cliConfig.getMix().setAutoMix(valueBool);
    }

    valueBool = optionalBoolean(ARG_AUTO_AGGREGATE_POSTMIX);
    if (valueBool != null) {
      cliConfig.getMix().setAutoAggregatePostmix(valueBool);
    }

    value = optionalOption(ARG_PUSHTX);
    if (value != null) {
      cliConfig.setPushtx(value);
    }

    valueBool = optionalBoolean(ARG_TOR);
    if (valueBool != null) {
      cliConfig.setTor(valueBool);
    }

    valueInt = optionalInt(ARG_CLIENTS);
    if (valueInt != null) {
      cliConfig.getMix().setClients(valueInt);
    }

    valueInt = optionalInt(ARG_CLIENT_DELAY);
    if (valueInt != null) {
      cliConfig.getMix().setClientDelay(valueInt);
    }

    valueInt = optionalInt(ARG_TX0_DELAY);
    if (valueInt != null) {
      cliConfig.getMix().setTx0Delay(valueInt);
    }

    valueInt = optionalInt(ARG_TX0_MAX_OUTPUTS);
    if (valueInt != null) {
      cliConfig.getMix().setTx0MaxOutputs(valueInt);
    }

    value = optionalOption(ARG_API_KEY);
    if (value != null) {
      cliConfig.setApiKey(value);
    }
  }

  public boolean isDebug() {
    Boolean value = optionalBoolean(ARG_DEBUG);
    if (value == null) {
      value = false;
    }
    return value;
  }

  public boolean isDebugClient() {
    Boolean value = optionalBoolean(ARG_DEBUG_CLIENT);
    if (value == null) {
      value = false;
    }
    return value;
  }

  private Collection<String> getPoolIdsByPriority() {
    List<String> poolIdsByPriority = new LinkedList<>();

    String poolIdsStr = optionalOption(ARG_POOL_ID);
    if (poolIdsStr != null && !poolIdsStr.isEmpty()) {
      String[] poolIdsSplit = poolIdsStr.split(",");
      poolIdsByPriority.addAll(Arrays.asList(poolIdsSplit));
    }
    return poolIdsByPriority;
  }

  private String getUtxo() {
    String utxo = requireOption(ARG_UTXO);
    Assert.notNull(utxo, "utxo is null");
    return utxo;
  }

  public boolean isUtxo() {
    return args.containsOption(ARG_UTXO);
  }

  public String getUtxoHash() {
    String utxo = getUtxo();
    String utxoSplit[] = utxo.split(UTXO_SEPARATOR);
    String utxoHash = utxoSplit[0];
    return utxoHash;
  }

  public long getUtxoIdx() {
    String utxo = getUtxo();
    String utxoSplit[] = utxo.split(UTXO_SEPARATOR);
    long utxoIdx = Long.parseLong(utxoSplit[1]);
    return utxoIdx;
  }

  public String getUtxoKey() {
    String utxoKey = requireOption(ARG_UTXO_KEY);
    Assert.notNull(utxoKey, "utxoKey is null");
    return utxoKey;
  }

  public long getUtxoBalance() {
    long utxoBalance;
    try {
      utxoBalance = Integer.parseInt(requireOption(ARG_UTXO_BALANCE));
    } catch (Exception e) {
      throw new IllegalArgumentException("Numeric value expected for option: " + ARG_UTXO_BALANCE);
    }
    return utxoBalance;
  }

  public String getSeedWords() throws NotifiableException {
    String seedWords = optionalOption(ARG_SEED_WORDS);
    if (seedWords == null) {
      seedWords = CliUtils.readUserInput(ARG_SEED_WORDS, true);
    }
    Assert.notNull(seedWords, "seedWords are null");
    return seedWords;
  }

  public String getSeedPassphrase() throws NotifiableException {
    String seedPassphrase = optionalOption(ARG_SEED_PASSPHRASE);
    if (seedPassphrase == null) {
      seedPassphrase = CliUtils.readUserInput(ARG_SEED_PASSPHRASE, true);
    }
    Assert.notNull(seedPassphrase, "seedPassphrase is null");
    return seedPassphrase;
  }

  public int getMixs() {
    final int mixs;
    try {
      mixs = Integer.parseInt(requireOption(ARG_MIXS, "1"));
      Assert.isTrue(mixs > 0, "mixs should be > 0");
    } catch (Exception e) {
      throw new IllegalArgumentException("Numeric value expected for option: " + ARG_MIXS);
    }
    return mixs;
  }

  public boolean isListPools() {
    Boolean listPools = optionalBoolean(ARG_LIST_POOLS);
    if (listPools == null) {
      listPools = false;
    }
    return listPools;
  }

  public boolean isAggregatePostmix() {
    return args.containsOption(ARG_AGGREGATE_POSTMIX);
  }

  public String getAggregatePostmix() {
    return optionalOption(ARG_AGGREGATE_POSTMIX);
  }

  public boolean isAutoTx0() {
    return args.containsOption(ARG_AUTO_TX0);
  }

  public static Integer getMainListen(String[] mainArgs) {
    return mainInteger(mainArgs, ARG_LISTEN, null, LISTEN_DEFAULT_PORT);
  }

  private String requireOption(String name, String defaultValue) {
    // arg not found
    if (!args.getOptionNames().contains(name)) {
      if (log.isDebugEnabled()) {
        log.debug("--" + name + "=" + defaultValue + " (default value)");
      }
      return defaultValue;
    }
    // --param (with no value) => "true"
    Iterator<String> iter = args.getOptionValues(name).iterator();
    if (!iter.hasNext()) {
      return "true";
    }
    return iter.next();
  }

  private String requireOption(String name) {
    String value = requireOption(name, null);
    if (value == null) {
      throw new IllegalArgumentException("Missing required option: " + name);
    }
    return value;
  }

  private String optionalOption(String name) {
    return requireOption(name, null);
  }

  private Boolean optionalBoolean(String name) {
    String value = optionalOption(name);
    return value != null ? Boolean.parseBoolean(value) : null;
  }

  private Integer optionalInt(String name) {
    String value = optionalOption(name);
    return value != null ? Integer.parseInt(value) : null;
  }

  private static String mainArg(
      String[] mainArgs,
      String name,
      String defaultValueWhenNotPresent,
      String defaultValueWhenPresent) {
    Optional<String> argFound =
        Stream.of(mainArgs).filter(s -> s.startsWith("--" + name)).findFirst();

    // arg not found
    if (!argFound.isPresent()) {
      return defaultValueWhenNotPresent;
    }

    // --param (with no value) => "true"
    String[] argSplit = argFound.get().split("=");
    if (argSplit.length == 1) {
      return defaultValueWhenPresent;
    }
    return argSplit[1];
  }

  private static Integer mainInteger(
      String[] mainArgs,
      String name,
      Integer defaultValueWhenNotPresent,
      Integer defaultValueWhenPresent) {
    String str =
        mainArg(
            mainArgs,
            name,
            defaultValueWhenNotPresent != null
                ? Integer.toString(defaultValueWhenNotPresent)
                : null,
            defaultValueWhenPresent != null ? Integer.toString(defaultValueWhenPresent) : null);
    if (str == null) {
      return null;
    }
    return Integer.parseInt(str);
  }
}
