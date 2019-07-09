package com.samourai.http.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class JavaHttpClient implements IHttpClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliTorClientService torClientService;
  private CliConfig cliConfig;
  private ObjectMapper objectMapper;
  private HttpClient httpClientShared;
  private HttpClient httpClientRegOut;

  public JavaHttpClient(CliTorClientService torClientService, CliConfig cliConfig) {
    this.torClientService = torClientService;
    this.cliConfig = cliConfig;
    this.objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    httpClientShared = null;
    httpClientRegOut = null;
  }

  @Override
  public synchronized <T> T getJson(String urlStr, Class<T> responseType) throws HttpException {
    final boolean isRegOut = false;
    try {
      HttpClient httpClient = getHttpClient(isRegOut);
      ContentResponse response = httpClient.GET(urlStr);

      T result = parseResponse(response, responseType);
      return result;
    } catch (Exception e) {
      clearHttpClient(isRegOut);
      if (log.isDebugEnabled()) {
        log.error("getJson failed: " + urlStr + ":" + e.getMessage());
      }
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      throw (HttpException) e;
    }
  }

  @Override
  public synchronized <T> T postJsonOverTor(String urlStr, Class<T> responseType, Object bodyObj)
      throws HttpException {
    final boolean isRegOut = true;
    try {
      HttpClient httpClient = getHttpClient(isRegOut);
      Request request = httpClient.POST(urlStr);

      String jsonBody = objectMapper.writeValueAsString(bodyObj);
      request.content(
          new StringContentProvider(
              MediaType.APPLICATION_JSON_VALUE, jsonBody, StandardCharsets.UTF_8));
      ContentResponse response = request.send();

      T result = parseResponse(response, responseType);
      return result;
    } catch (Exception e) {
      clearHttpClient(isRegOut);
      if (log.isDebugEnabled()) {
        log.error("postJsonOverTor failed: " + urlStr, e);
      }
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      throw (HttpException) e;
    }
  }

  @Override
  public synchronized <T> T postUrlEncoded(
      String urlStr, Class<T> responseType, Map<String, String> body) throws HttpException {
    final boolean isRegOut = false;
    try {
      HttpClient httpClient = getHttpClient(isRegOut);
      Request request = httpClient.POST(urlStr);

      request.content(new FormContentProvider(computeBodyFields(body)));
      ContentResponse response = request.send();

      T result = parseResponse(response, responseType);
      return result;
    } catch (Exception e) {
      clearHttpClient(isRegOut);
      if (log.isDebugEnabled()) {
        log.error("postUrlEncoded failed: " + urlStr, e);
      }
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      throw (HttpException) e;
    }
  }

  private Fields computeBodyFields(Map<String, String> body) {
    Fields fields = new Fields();
    for (Map.Entry<String, String> entry : body.entrySet()) {
      fields.put(entry.getKey(), entry.getValue());
    }
    return fields;
  }

  private <T> T parseResponse(ContentResponse response, Class<T> responseType) throws Exception {
    T result = null;
    if (responseType != null) {
      result = objectMapper.readValue(response.getContent(), responseType);
    }
    return result;
  }

  private HttpClient getHttpClient(boolean isRegisterOutput) throws Exception {
    if (!isRegisterOutput) {
      if (httpClientShared == null) {
        if (log.isDebugEnabled()) {
          log.debug("+httpClientShared");
        }
        httpClientShared = computeHttpClient(isRegisterOutput);
      }
      return httpClientShared;
    } else {
      if (httpClientRegOut == null) {
        if (log.isDebugEnabled()) {
          log.debug("+httpClientRegOut");
        }
        httpClientRegOut = computeHttpClient(isRegisterOutput);
      }
      return httpClientRegOut;
    }
  }

  private HttpClient computeHttpClient(boolean isRegisterOutput) throws Exception {
    HttpClient httpClient =
        CliUtils.computeHttpClient(isRegisterOutput, torClientService, cliConfig.getCliProxy());
    httpClient.start();
    return httpClient;
  }

  private void clearHttpClient(boolean isRegisterOutput) {
    if (!isRegisterOutput) {
      if (httpClientShared != null) {
        try {
          httpClientShared.stop();
        } catch (Exception e) {
        }
        if (log.isDebugEnabled()) {
          log.debug("clear httpClientShared");
        }
        httpClientShared = null;
      }
    } else {
      if (httpClientRegOut != null) {
        try {
          httpClientRegOut.stop();
        } catch (Exception e) {
        }
        if (log.isDebugEnabled()) {
          log.debug("clear httpClientRegOut");
        }
        httpClientRegOut = null;
      }
    }
  }
}
