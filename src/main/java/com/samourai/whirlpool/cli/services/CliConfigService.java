package com.samourai.whirlpool.cli.services;

import com.samourai.whirlpool.cli.beans.CliStatus;
import com.samourai.whirlpool.cli.beans.Encrypted;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.cli.utils.EncryptUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DefaultPropertiesPersister;

@Service
public class CliConfigService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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

  public synchronized String initialize(Encrypted seedWordsEncrypted) throws Exception {
    if (!CliStatus.NOT_INITIALIZED.equals(cliStatus)) {
      throw new NotifiableException("CLI is already initialized");
    }

    // serialize seedWordsEncrypted
    String seedWordsEncryptedStr = EncryptUtils.serializeEncrypted(seedWordsEncrypted);

    // generate apiKey
    String apiKey = CliUtils.generateUniqueString();

    // save configuration file
    Map<String, String> entries = new HashMap<>();
    entries.put(KEY_APIKEY, apiKey);
    entries.put(KEY_SEED, seedWordsEncryptedStr);
    save(entries);

    // restart needed
    this.setCliStatus(
        CliStatus.NOT_READY,
        "CLI restart required. Wallet inizialization success. Please restart CLI.");

    log.info("⣿ RESTART REQUIRED ⣿ Wallet inizialization success. Please restart CLI.");
    return apiKey;
  }

  protected synchronized void save(Map<String, String> entries) throws Exception {
    File f = new File("application-default.properties");
    if (!f.exists()) {
      f.createNewFile();
    }

    Properties props = new Properties();
    props.load(new FileInputStream(f));

    for (Entry<String, String> entry : entries.entrySet()) {
      props.setProperty(entry.getKey(), entry.getValue());
    }

    OutputStream out = new FileOutputStream(f);
    DefaultPropertiesPersister p = new DefaultPropertiesPersister();
    p.store(props, out, "Updated by application");
  }
}
