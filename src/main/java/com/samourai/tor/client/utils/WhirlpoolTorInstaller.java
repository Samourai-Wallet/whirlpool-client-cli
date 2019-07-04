// TODO https://github.com/thaliproject/Tor_Onion_Proxy_Library/pull/127
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samourai.tor.client.utils;

import com.msopentech.thali.toronionproxy.FileUtilities;
import com.msopentech.thali.toronionproxy.OsData;
import com.msopentech.thali.toronionproxy.TorConfig;
import com.msopentech.thali.toronionproxy.TorInstaller;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WhirlpoolTorInstaller extends TorInstaller {
  private static final Logger LOG = LoggerFactory.getLogger(WhirlpoolTorInstaller.class);
  private final TorConfig config;
  private boolean useExecutableFromZip;

  public WhirlpoolTorInstaller(TorConfig config, boolean useExecutableFromZip) {
    this.config = config;
    this.useExecutableFromZip = useExecutableFromZip;
  }

  private static String getPathToTorExecutable() {
    String path = "native/";
    switch (OsData.getOsType()) {
      case WINDOWS:
        return path + "windows/x86/";
      case MAC:
        return path + "osx/x64/";
      case LINUX_32:
        return path + "linux/x86/";
      case LINUX_64:
        return path + "linux/x64/";
      default:
        throw new RuntimeException("We don't support Tor on this OS");
    }
  }

  public void setup() throws IOException {
    LOG.info("Setting up tor");
    LOG.info("Installing resources: geoip=" + this.config.getGeoIpFile().getAbsolutePath());
    FileUtilities.cleanInstallOneFile(
        this.getAssetOrResourceByName("geoip"), this.config.getGeoIpFile());
    FileUtilities.cleanInstallOneFile(
        this.getAssetOrResourceByName("geoip6"), this.config.getGeoIpv6File());

    if (useExecutableFromZip) {
      setupTorExecutable();
    } else {
      LOG.info(
          "Using existing tor executable: " + this.config.getTorExecutableFile().getAbsolutePath());
    }
  }

  protected void setupTorExecutable() throws IOException {
    LOG.info("Installing tor executable: " + this.config.getTorExecutableFile().getAbsolutePath());
    File torParent = this.config.getTorExecutableFile().getParentFile();
    FileUtilities.extractContentFromZip(
        torParent.exists() ? torParent : this.config.getTorExecutableFile(),
        this.getAssetOrResourceByName(getPathToTorExecutable() + "tor.zip"));
    FileUtilities.setPerms(this.config.getTorExecutableFile());
  }

  public void updateTorConfigCustom(String content) throws IOException, TimeoutException {
    PrintWriter printWriter = null;

    try {
      LOG.info("Updating torrc file; torrc =" + this.config.getTorrcFile().getAbsolutePath());
      printWriter =
          new PrintWriter(new BufferedWriter(new FileWriter(this.config.getTorrcFile(), true)));
      printWriter.println(
          "PidFile " + (new File(this.config.getDataDir(), "pid")).getAbsolutePath());
      printWriter.print(content);
    } finally {
      if (printWriter != null) {
        printWriter.close();
      }
    }
  }

  public InputStream openBridgesStream() throws IOException {
    throw new UnsupportedOperationException();
  }
}
