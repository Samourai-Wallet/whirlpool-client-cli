package com.samourai.whirlpool.cli.config.security;

import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableWebSecurity
public class CliWebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // disable CSRF
    http.csrf().disable()

        // authorize REST API
        .authorizeRequests()
        .antMatchers(CliApiEndpoint.REST_ENDPOINTS)
        .permitAll()

        // reject others
        .anyRequest()
        .denyAll();
  }
}
