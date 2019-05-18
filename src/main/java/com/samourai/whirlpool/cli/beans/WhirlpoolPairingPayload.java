package com.samourai.whirlpool.cli.beans;

import com.samourai.wallet.pairing.payload.PairingNetwork;
import com.samourai.wallet.pairing.payload.PairingPayload;
import com.samourai.wallet.pairing.payload.PairingType;
import com.samourai.wallet.pairing.payload.PairingVersion;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhirlpoolPairingPayload extends PairingPayload {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public WhirlpoolPairingPayload(PairingVersion version, PairingNetwork network, String mnemonic) {
    super(PairingType.WHIRLPOOL_GUI, version, network, mnemonic);
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
    if (!PairingVersion.V1_0_0.equals(getPairing().getVersion())) {
      throw new NotifiableException("Unsupported pairing.version");
    }
  }
}
