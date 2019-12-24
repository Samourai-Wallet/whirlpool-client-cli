package com.samourai.whirlpool.cli.config;

import com.samourai.wallet.hd.java.HD_WalletFactoryJava;
import com.samourai.wallet.segwit.bech32.Bech32UtilGeneric;
import com.samourai.whirlpool.cli.api.protocol.beans.ApiCliConfig;
import java.lang.invoke.MethodHandles;
import org.apache.catalina.connector.Connector;
import org.bitcoinj.core.NetworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableCaching
public class CliServicesConfig {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public CliServicesConfig() {}

  @Bean
  TaskExecutor taskExecutor() {
    return new SimpleAsyncTaskExecutor();
  }

  @Bean
  HD_WalletFactoryJava hdWalletFactory() {
    return HD_WalletFactoryJava.getInstance();
  }

  @Bean
  Bech32UtilGeneric bech32Util() {
    return Bech32UtilGeneric.getInstance();
  }

  @Bean
  NetworkParameters networkParameters(CliConfig cliConfig) {
    return cliConfig.getServer().getParams();
  }

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOrigin("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("OPTIONS");
    config.addAllowedMethod("HEAD");
    config.addAllowedMethod("GET");
    config.addAllowedMethod("PUT");
    config.addAllowedMethod("POST");
    config.addAllowedMethod("DELETE");
    config.addAllowedMethod("PATCH");
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }

  @Bean
  @ConditionalOnProperty(name = ApiCliConfig.KEY_API_HTTP_ENABLE, havingValue = "true")
  public ServletWebServerFactory httpServer(CliConfig cliConfig) {
    // https not required => configure HTTP server
    int httpPort = cliConfig.getApi().getHttpPort();
    if (log.isDebugEnabled()) {
      log.debug("Enabling API over HTTP... httpPort=" + httpPort);
    }
    Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
    connector.setPort(httpPort);
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
    factory.addAdditionalTomcatConnectors(connector);
    return factory;
  }
}
