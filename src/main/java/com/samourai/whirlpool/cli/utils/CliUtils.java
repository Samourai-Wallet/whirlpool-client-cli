package com.samourai.whirlpool.cli.utils;

import com.samourai.tor.client.JavaTorConnexion;
import com.samourai.whirlpool.cli.beans.CliProxy;
import com.samourai.whirlpool.cli.beans.CliProxyProtocol;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.io.Console;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliUtils {
  private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String LOG_SEPARATOR = "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿";
  public static final String SPRING_PROFILE_TESTING = "testing";

  public static String generateUniqueString() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public static void waitUserAction(String message) throws NotifiableException {
    Console console = System.console();
    if (console != null) {
      log.info("⣿ ACTION REQUIRED ⣿ " + message);
      log.info("Press <ENTER> when ready:");
      console.readLine();
    } else {
      throw new NotifiableException("⣿ ACTION REQUIRED ⣿ " + message);
    }
  }

  public static String readUserInputRequired(
      String message, boolean secret, String[] allowedValues) {
    message = "⣿ INPUT REQUIRED ⣿ " + message;
    String input;
    do {
      input = readUserInput(message, secret, true);
    } while (input == null || !ArrayUtils.contains(allowedValues, input));
    return input;
  }

  public static String readUserInputRequired(String message, boolean secret) {
    message = "⣿ INPUT REQUIRED ⣿ " + message;
    return readUserInput(message, secret, true);
  }

  public static String readUserInput(String message, boolean secret, boolean scannerFallback) {
    Console console = System.console();
    String inviteMessage = message + ">";

    // read line
    String line = null;
    if (console != null) {
      console.printf(inviteMessage);
      line = secret ? new String(console.readPassword()) : console.readLine();
    } else if (scannerFallback) {
      // allow console redirection
      Scanner input = new Scanner(System.in);
      System.out.print(inviteMessage);
      line = input.nextLine();
    }
    if (line != null) {
      line = line.trim();
      if (line.isEmpty()) {
        return null;
      }
    }
    return line;
  }

  public static Character readChar() {
    Console console = System.console();
    if (console != null) {
      try {
        return (char) console.reader().read();
      } catch (IOException e) {
        return null;
      }
    }
    return null;
  }

  public static void notifyError(String message) {
    log.error("⣿ ERROR ⣿ " + message);
  }

  public static Optional<CliProxy> computeProxy(final String proxy) {
    if (StringUtils.isEmpty(proxy)) {
      return Optional.empty();
    }
    String[] splitProtocol = proxy.split("://");
    if (splitProtocol.length != 2) {
      throw new IllegalArgumentException("Invalid proxy: " + proxy);
    }
    CliProxyProtocol proxyProtocol =
        CliProxyProtocol.find(splitProtocol[0].toUpperCase())
            .orElseThrow(
                () -> new IllegalArgumentException("Unsupported proxy protocol: " + proxy));
    String[] split = splitProtocol[1].split(":");
    if (split.length != 2) {
      throw new IllegalArgumentException("Invalid proxy: " + proxy);
    }
    try {
      int port = Integer.parseInt(split[1]);
      if (port < 1) {
        throw new IllegalArgumentException("Invalid proxy port: " + proxy);
      }
      String host = split[0];
      if (StringUtils.isEmpty(host)) {
        throw new IllegalArgumentException("Invalid proxy host: " + proxy);
      }
      return Optional.of(new CliProxy(proxyProtocol, host, port));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid proxy: " + proxy);
    }
  }

  public static void useProxy(CliProxy cliProxy) {
    String portStr = Integer.toString(cliProxy.getPort()); // important cast
    switch (cliProxy.getProtocol()) {
      case SOCKS:
        System.setProperty("socksProxyHost", cliProxy.getHost());
        System.setProperty("socksProxyPort", portStr);
        break;
      case HTTP:
        System.setProperty("http.proxyHost", cliProxy.getHost());
        System.setProperty("http.proxyPort", portStr);
        System.setProperty("https.proxyHost", cliProxy.getHost());
        System.setProperty("https.proxyPort", portStr);
        break;
    }
  }

  public static HttpClient computeHttpClient(
      boolean isRegisterOutput,
      CliTorClientService torClientService,
      Optional<CliProxy> cliProxyDefault)
      throws NotifiableException {
    // use torConnexion when available, otherwise cliProxyDefault
    Optional<JavaTorConnexion> torConnexion = torClientService.getTorConnexion(isRegisterOutput);
    Optional<CliProxy> cliProxyOptional =
        torConnexion.isPresent() ? Optional.of(torConnexion.get().getTorProxy()) : cliProxyDefault;
    return computeHttpClient(cliProxyOptional);
  }

  protected static HttpClient computeHttpClient(Optional<CliProxy> cliProxyOptional) {
    // we use jetty for proxy SOCKS support
    HttpClient jettyHttpClient = new HttpClient(new SslContextFactory());
    // jettyHttpClient.setSocketAddressResolver(new MySocketAddressResolver());

    // prevent user-agent tracking
    jettyHttpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, ClientUtils.USER_AGENT));

    // proxy
    if (cliProxyOptional.isPresent()) {
      CliProxy cliProxy = cliProxyOptional.get();
      if (log.isDebugEnabled()) {
        log.debug("httpClient.proxy=" + cliProxy);
      }
      ProxyConfiguration.Proxy jettyProxy = cliProxy.computeJettyProxy();
      jettyHttpClient.getProxyConfiguration().getProxies().add(jettyProxy);
    }
    return jettyHttpClient;
  }

  public static List<String> exec(String cmd) throws Exception {
    List<String> lines = new ArrayList<>();
    Process proc = null;
    Scanner scanner = null;
    try {
      proc = Runtime.getRuntime().exec(cmd);

      scanner = new Scanner(proc.getInputStream());
      while (scanner.hasNextLine()) {
        lines.add(scanner.nextLine());
      }

      int exit = proc.waitFor();
      if (exit != 0) {
        throw new RuntimeException("exec [" + cmd + "] returned error code: " + exit);
      }
    } finally {
      if (proc != null) {
        proc.destroy();
      }
      if (scanner != null) {
        scanner.close();
      }
    }
    return lines;
  }
}
