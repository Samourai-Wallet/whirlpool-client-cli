package com.samourai.tor.client;

import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.client.exception.NotifiableException;

public interface JavaTorConnexion {

  CliProxy getTorProxy(boolean isRegisterOutput) throws NotifiableException;
}
