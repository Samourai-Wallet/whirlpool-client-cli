package com.samourai.whirlpool.cli.beans;

import com.samourai.wallet.api.pairing.PairingNetwork;
import com.samourai.wallet.api.pairing.PairingPayload;
import com.samourai.wallet.api.pairing.PairingType;
import com.samourai.wallet.api.pairing.PairingVersion;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolPairingPayload extends PairingPayload {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public WhirlpoolPairingPayload() {
    super();
  }

  public WhirlpoolPairingPayload(
      PairingVersion version, PairingNetwork network, String mnemonic, Boolean passphrase) {
    super(PairingType.WHIRLPOOL_GUI, version, network, mnemonic, passphrase);
  }

  public static WhirlpoolPairingPayload parse(String pairingPayloadStr) throws NotifiableException {
    WhirlpoolPairingPayload pairingPayload;
    try {
      pairingPayload = ClientUtils.fromJson(pairingPayloadStr, WhirlpoolPairingPayload.class);
    } catch (NotifiableException e) {
      throw e;
    } catch (Exception e) {
      log.error("", e);
      throw new NotifiableException("Invalid pairing payload");
    }

    // passphrase=true for V1
    if (pairingPayload.getPairing().getPassphrase() == null
        && PairingVersion.V1_0_0.equals(pairingPayload.getPairing().getVersion())) {
      pairingPayload.getPairing().setPassphrase(true);
    }
    pairingPayload.validate();
    return pairingPayload;
  }

  protected void validate() throws NotifiableException {
    // main validation
    try {
      super.validate();
    } catch (Exception e) {
      throw new NotifiableException(e.getMessage());
    }

    // whirlpool validation
    if (!PairingType.WHIRLPOOL_GUI.equals(getPairing().getType())) {
      throw new NotifiableException("Unsupported pairing.type");
    }
    if (!PairingVersion.V1_0_0.equals(getPairing().getVersion())
        && !PairingVersion.V2_0_0.equals(getPairing().getVersion())) {
      throw new NotifiableException("Unsupported pairing.version");
    }
    if (getPairing().getPassphrase() == null) {
      throw new NotifiableException("Invalid pairing.passphrase");
    }
  }
}
