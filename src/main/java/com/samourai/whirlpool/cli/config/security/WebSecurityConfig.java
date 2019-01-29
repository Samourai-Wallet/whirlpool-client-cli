package com.samourai.whirlpool.cli.config.security;

import com.samourai.whirlpool.cli.api.rest.wallet.DepositController;
import com.samourai.whirlpool.cli.api.rest.wallet.WalletController;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
  private static final String[] REST_ENDPOINTS =
      new String[] {DepositController.ENDPOINT, WalletController.ENDPOINT};

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    // disable csrf for mixing
    http.authorizeRequests()
        .antMatchers(REST_ENDPOINTS)
        .permitAll()

        // reject others
        .anyRequest()
        .denyAll();
  }
}
