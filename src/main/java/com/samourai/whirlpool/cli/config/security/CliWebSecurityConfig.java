package com.samourai.whirlpool.cli.config.security;

import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import com.samourai.whirlpool.cli.config.CliConfig;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
public class CliWebSecurityConfig extends WebSecurityConfigurerAdapter {
  private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliConfig cliConfig;

  public CliWebSecurityConfig(CliConfig cliConfig) {
    this.cliConfig = cliConfig;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    boolean requireHttps = cliConfig.getApi().isRequireHttps();
    if (log.isDebugEnabled()) {
      log.debug("Configuring REST API: requireHttps=" + requireHttps);
    }

    // disable CSRF
    http.csrf()
        .disable()

        // authorize REST API
        .authorizeRequests()
        .antMatchers(CliApiEndpoint.REST_ENDPOINTS)
        .permitAll()

        // reject others
        .anyRequest()
        .denyAll();

    if (requireHttps) {
      http.requiresChannel().anyRequest().requiresSecure();
    }
  }
}
