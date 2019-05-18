package com.samourai.whirlpool.cli.services;

import com.samourai.wallet.pairing.payload.PairingNetwork;
import com.samourai.whirlpool.cli.api.protocol.beans.ApiCliConfig;
import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.cli.beans.WhirlpoolPairingPayload;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DefaultPropertiesPersister;

@Service
public class CliConfigService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String CLI_CONFIG_FILENAME = "whirlpool-cli-config.properties";
  private static final String KEY_APIKEY = "cli.apiKey";
  private static final String KEY_SEED = "cli.seed";

  private CliConfig cliConfig;
  private CliStatus cliStatus;
  private String cliMessage;

  public CliConfigService(CliConfig cliConfig) {
    this.cliConfig = cliConfig;
    this.cliStatus = CliStatus.NOT_INITIALIZED;
    if (!Strings.isEmpty(cliConfig.getSeed()) && !Strings.isEmpty(cliConfig.getApiKey())) {
      this.setCliStatus(CliStatus.READY);
    }
  }

  public CliStatus getCliStatus() {
    return cliStatus;
  }

  public String getCliMessage() {
    return cliMessage;
  }

  protected void setCliStatus(CliStatus cliStatus) {
    this.setCliStatus(cliStatus, null);
  }

  protected void setCliStatus(CliStatus cliStatus, String cliMessage) {
    this.cliStatus = cliStatus;
    this.cliMessage = cliMessage;
  }

  public boolean isCliStatusReady() {
    return CliStatus.READY.equals(cliStatus);
  }

  public boolean isCliStatusNotInitialized() {
    return CliStatus.NOT_INITIALIZED.equals(cliStatus);
  }

  public String initialize(String pairingPayloadStr) throws NotifiableException {
    // parse payload
    WhirlpoolPairingPayload pairingPayload = WhirlpoolPairingPayload.parse(pairingPayloadStr);

    // initialize
    String encryptedMnemonic = pairingPayload.getPairing().getMnemonic();
    PairingNetwork pairingNetwork = pairingPayload.getPairing().getNetwork();
    WhirlpoolServer whirlpoolServer =
        PairingNetwork.MAINNET.equals(pairingNetwork)
            ? WhirlpoolServer.MAINNET
            : WhirlpoolServer.TESTNET;
    return initialize(encryptedMnemonic, whirlpoolServer);
  }

  private synchronized String initialize(String encryptedMnemonic, WhirlpoolServer whirlpoolServer)
      throws NotifiableException {
    if (whirlpoolServer == null) {
      throw new NotifiableException("Invalid server");
    }
    if (StringUtils.isEmpty(encryptedMnemonic)) {
      throw new NotifiableException("Invalid mnemonic");
    }

    // generate apiKey
    String apiKey = CliUtils.generateUniqueString();

    // save configuration file
    Properties props = new Properties();
    props.put(KEY_APIKEY, apiKey);
    props.put(KEY_SEED, encryptedMnemonic);
    props.put(ApiCliConfig.KEY_SERVER, whirlpoolServer.name());
    try {
      save(props);
    } catch (Exception e) {
      log.error("", e);
      throw new NotifiableException("Unable to save CLI configuration");
    }

    // restart needed
    this.setCliStatusNotReady("CLI restart required. Wallet inizialization success.");
    return apiKey;
  }

  public Properties loadEntries() throws Exception {
    Resource resource = new FileSystemResource(getConfigurationFile());
    Properties props = PropertiesLoaderUtils.loadProperties(resource);
    return props;
  }

  public synchronized void setApiConfig(ApiCliConfig apiCliConfig) throws Exception {
    Properties props = loadEntries();

    apiCliConfig.toProperties(props);

    // log
    for (Entry<Object, Object> entry : props.entrySet()) {
      log.info("set " + entry.getKey() + ": " + entry.getValue());
    }

    // save
    save(props);

    // restart needed
    this.setCliStatusNotReady("CLI restart required. Configuration updated.");
  }

  public synchronized void resetConfiguration() throws Exception {
    log.info("resetConfiguration");

    File f = getConfigurationFile();
    if (f.exists()) {
      if (!f.delete()) {
        throw new NotifiableException("Could not delete " + f.getAbsolutePath());
      }
    }

    // restart needed
    this.setCliStatusNotReady("CLI restart required. Configuration reset.");
  }

  public void setCliStatusNotReady(String error) {
    this.setCliStatus(CliStatus.NOT_READY, error);
  }

  protected synchronized void save(Properties props) throws Exception {
    if (props.isEmpty()) {
      throw new IllegalArgumentException("Configuration to save is empty");
    }

    File f = getConfigurationFile();
    if (!f.exists()) {
      f.createNewFile();
    }

    OutputStream out = new FileOutputStream(f);
    DefaultPropertiesPersister p = new DefaultPropertiesPersister();
    p.store(props, out, "Updated by application");
  }

  private File getConfigurationFile() {
    return new File(CLI_CONFIG_FILENAME);
  }
}
