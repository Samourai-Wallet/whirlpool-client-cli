package com.samourai.whirlpool.cli.api.rest;

import com.samourai.api.client.beans.UnspentResponse.UnspentOutput;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
  private static final String ENDPOINT = "/test";

  @RequestMapping(value = ENDPOINT)
  public UnspentOutput errorHtml(
      @RequestParam(value = "name", defaultValue = "World") String name) {
    return new UnspentOutput();
  }
}
