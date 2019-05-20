package com.samourai.http.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.samourai.tor.client.JavaTorConnexion;
import com.samourai.wallet.api.backend.beans.HttpException;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.client.utils.ClientUtils;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaHttpClient implements IHttpClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private CliTorClientService torClientService;
  private ObjectMapper objectMapper;

  public JavaHttpClient(CliTorClientService torClientService) {
    this.torClientService = torClientService;
    this.objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Override
  public <T> T getJson(String urlStr, Class<T> responseType) throws HttpException {
    Optional<JavaTorConnexion> torConnexion = torClientService.getTorConnexion(false);
    try {
      HttpRequest request;
      if (torConnexion.isPresent()) {
        // use TOR
        URL url = torConnexion.get().getUrl(urlStr);
        request = HttpRequest.get(url);
      } else {
        // standard connexion
        request = HttpRequest.get(urlStr);
      }
      setHeaders(request);
      execute(request);
      T result = objectMapper.readValue(request.bytes(), responseType);
      // keep sharedTorConnexion open
      return result;
    } catch (Exception e) {
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      throw (HttpException) e;
    } finally {
      if (torConnexion.isPresent()) {
        torConnexion.get().close();
      }
    }
  }

  @Override
  public <T> T postJsonOverTor(String urlStr, Class<T> responseType, Object bodyObj)
      throws HttpException {
    Optional<JavaTorConnexion> torConnexion = torClientService.getTorConnexion(true);
    try {
      String jsonBody = objectMapper.writeValueAsString(bodyObj);
      HttpRequest request;
      if (torConnexion.isPresent()) {
        // use TOR
        URL url = torConnexion.get().getUrl(urlStr);
        request = HttpRequest.post(url);
      } else {
        // standard connexion
        request = HttpRequest.post(urlStr);
      }
      setHeaders(request);
      request.contentType(HttpRequest.CONTENT_TYPE_JSON).send(jsonBody.getBytes());
      execute(request);
      T result = objectMapper.readValue(request.bytes(), responseType);
      return result;
    } catch (Exception e) {
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      throw (HttpException) e;
    } finally {
      if (torConnexion.isPresent()) {
        torConnexion.get().close();
      }
    }
  }

  @Override
  public <T> T postUrlEncoded(String urlStr, Class<T> responseType, Map<String, String> body)
      throws HttpException {
    Optional<JavaTorConnexion> torConnexion = torClientService.getTorConnexion(false);
    String bodyUrlEncoded = HttpRequest.append("", body).substring(1); // remove starting '?'
    try {
      HttpRequest request;
      if (torConnexion.isPresent()) {
        // use TOR
        URL url = torConnexion.get().getUrl(urlStr);
        request = HttpRequest.post(url);
      } else {
        // standard connexion
        request = HttpRequest.post(urlStr);
      }
      setHeaders(request);
      request.contentType(HttpRequest.CONTENT_TYPE_FORM).send(bodyUrlEncoded.getBytes());
      execute(request);
      T result = objectMapper.readValue(request.bytes(), responseType);
      return result;
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.error("postUrlEncoded failed", e);
      }
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      throw (HttpException) e;
    } finally {
      if (torConnexion.isPresent()) {
        torConnexion.get().close();
      }
    }
  }

  private void setHeaders(HttpRequest request) {
    request.header(HttpRequest.HEADER_USER_AGENT, ClientUtils.USER_AGENT);
  }

  private void execute(HttpRequest request) throws HttpException {
    if (!request.ok()) {
      throw new HttpException(
          new Exception(
              "httpRequest failed: statusCode="
                  + request.code()
                  + " for "
                  + request.method()
                  + " "
                  + request.url()),
          request.body());
    }
  }
}
