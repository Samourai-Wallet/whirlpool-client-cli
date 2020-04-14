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
import com.samourai.whirlpool.cli.utils.CliUtils;
import java.io.*;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WhirlpoolTorInstaller extends TorInstaller {
  private static final Logger LOG = LoggerFactory.getLogger(WhirlpoolTorInstaller.class);
  private final TorConfig config;
  private boolean useExecutableFromZip;

  public WhirlpoolTorInstaller(String torDir, Optional<File> torExecutable, int fileCreationTimeout)
      throws Exception {
    this.config = computeTorConfig(torDir, torExecutable, fileCreationTimeout);
    this.useExecutableFromZip = !torExecutable.isPresent();
  }

  private static TorConfig computeTorConfig(
      String dirName, Optional<File> torExecutable, int fileCreationTimeout) throws Exception {
    File dir = Files.createTempDirectory(dirName).toFile();
    dir.deleteOnExit();

    TorConfig.Builder torConfigBuilder = new TorConfig.Builder(dir, dir).homeDir(dir);

    if (torExecutable.isPresent()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "configuring tor for external executable: " + torExecutable.get().getAbsolutePath());
      }
      // use existing local Tor instead of embedded one
      torConfigBuilder.torExecutable(torExecutable.get());
    }
    torConfigBuilder.fileCreationTimeout(fileCreationTimeout);

    TorConfig torConfig = torConfigBuilder.build();
    return torConfig;
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

    // detect runtime errors on tor executable (ie "error while loading shared libraries...")
    try {
      CliUtils.exec(this.config.getTorExecutableFile().getAbsolutePath() + " --help");
    } catch (Exception e) {
      throw new IOException("Tor executable error: " + e.getMessage());
    }
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

  public TorConfig getConfig() {
    return config;
  }
}
