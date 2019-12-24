package com.samourai.whirlpool.cli.config.security;

import com.samourai.whirlpool.cli.config.CliConfig;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/** Configure HTTPS server. */
@Component
public class CliWebServerFactoryCustomizer
    implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  private CliConfig cliConfig;

  public CliWebServerFactoryCustomizer(CliConfig cliConfig) {
    this.cliConfig = cliConfig;
  }

  @Override
  public void customize(TomcatServletWebServerFactory factory) {
    // configure HTTPS port
    factory.setPort(cliConfig.getApi().getPort());
  }
}
