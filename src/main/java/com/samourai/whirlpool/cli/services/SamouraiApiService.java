package com.samourai.whirlpool.cli.services;

import com.samourai.api.client.SamouraiApi;
import com.samourai.http.client.IHttpClient;
import org.springframework.stereotype.Service;

@Service
public class SamouraiApiService extends SamouraiApi {

  public SamouraiApiService(IHttpClient httpClient) {
    super(httpClient);
  }
}
