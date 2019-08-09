package com.samourai.whirlpool.cli.services;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.samourai.api.client.SamouraiApi;
import com.samourai.http.client.IHttpClient;
import com.samourai.wallet.api.backend.beans.RefreshTokenResponse;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.utils.OAuthUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamouraiApiService extends SamouraiApi {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String ARG_ACCESS_TOKEN = "at";

  private DecodedJWT accessToken;
  private DecodedJWT refreshToken;

  public SamouraiApiService(IHttpClient httpClient, CliConfig cliConfig, String backendApiKey) {
    super(httpClient, cliConfig.computeBackendUrl(), backendApiKey);
  }

  @Override
  protected String computeAuthUrl(String url) throws Exception {
    url = super.computeAuthUrl(url);

    if (getApiKey() == null) {
      // no  apiKey => no auth
      return url;
    }
    url += (url.contains("?") ? "&" : "?") + ARG_ACCESS_TOKEN + "=" + computeAccessToken();
    return url;
  }

  private String computeAccessToken() throws Exception {
    if (accessToken != null) {
      boolean valid = OAuthUtils.validate(accessToken);
      if (log.isDebugEnabled()) {
        log.debug(
            "accessToken is "
                + (valid ? "VALID" : "EXPIRED")
                + ", expiresAt="
                + accessToken.getExpiresAt());
      }
      if (valid) {
        // accessToken is valid
        return accessToken.getToken();
      }
    }

    newAccessToken();
    return accessToken.getToken();
  }

  public void newAccessToken() throws Exception {
    if (refreshToken != null) {
      boolean valid = OAuthUtils.validate(refreshToken);
      if (log.isDebugEnabled()) {
        log.debug(
            "refreshToken is "
                + (valid ? "VALID" : "EXPIRED")
                + ", expiresAt="
                + refreshToken.getExpiresAt());
      }
      if (valid) {
        // refreshToken is valid => refresh
        String accessTokenStr = tokenRefresh(refreshToken.getToken());
        this.accessToken = OAuthUtils.decode(accessTokenStr);
        return;
      }
    }

    // authenticate
    RefreshTokenResponse.Authorization auth = tokenAuthenticate();
    this.accessToken = OAuthUtils.decode(auth.access_token);
    this.refreshToken = OAuthUtils.decode(auth.refresh_token);
  }
}
