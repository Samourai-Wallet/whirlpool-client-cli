package com.samourai.whirlpool.cli.beans;

import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolServer;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliPairingPayload {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String EXPECTED_TYPE = "whirlpool.gui";
  private static final String EXPECTED_VERSION = "1.0.0";

  private CliPairingValue pairing;

  public static CliPairingPayload parse(String pairingPayloadStr) throws NotifiableException {
    CliPairingPayload pairingPayload;
    try {
      pairingPayload = ClientUtils.fromJson(pairingPayloadStr, CliPairingPayload.class);
    } catch (NotifiableException e) {
      throw e;
    } catch (Exception e) {
      log.error("", e);
      throw new NotifiableException("Invalid pairing payload");
    }
    pairingPayload.validate();
    return pairingPayload;
  }

  private void validate() throws NotifiableException {
    if (pairing == null) {
      throw new NotifiableException("Invalid pairing");
    }
    pairing.validate();
  }

  public CliPairingValue getPairing() {
    return pairing;
  }

  public void setPairing(CliPairingValue pairing) {
    this.pairing = pairing;
  }

  public static class CliPairingValue {
    private String type;
    private String version;
    private CliPairingNetwork network;
    private String mnemonic;

    public void validate() throws NotifiableException {
      if (type == null || !EXPECTED_TYPE.equals(type)) {
        throw new NotifiableException("Invalid pairing.type");
      }

      if (version == null || !EXPECTED_VERSION.equals(version)) {
        throw new NotifiableException("Invalid pairing.version");
      }

      if (network == null) {
        throw new NotifiableException("Invalid pairing.network");
      }

      if (StringUtils.isEmpty(mnemonic)) {
        throw new NotifiableException("Invalid pairing.mnemonic");
      }
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public CliPairingNetwork getNetwork() {
      return network;
    }

    public void setNetwork(CliPairingNetwork network) {
      this.network = network;
    }

    public String getMnemonic() {
      return mnemonic;
    }

    public void setMnemonic(String mnemonic) {
      this.mnemonic = mnemonic;
    }
  }

  public enum CliPairingNetwork {
    mainnet(WhirlpoolServer.MAIN),
    testnet(WhirlpoolServer.TEST);

    private WhirlpoolServer whirlpoolServer;

    CliPairingNetwork(WhirlpoolServer whirlpoolServer) {
      this.whirlpoolServer = whirlpoolServer;
    }

    public WhirlpoolServer getWhirlpoolServer() {
      return whirlpoolServer;
    }

    public static Optional<CliPairingNetwork> find(String value) {
      try {
        return Optional.of(valueOf(value));
      } catch (Exception e) {
        return Optional.empty();
      }
    }
  }
}
