package com.samourai.http.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.samourai.tor.client.JavaTorConnexion;
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
  public <T> T parseJson(String urlStr, Class<T> entityClass) throws HttpException {
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
      execute(request);
      T result = objectMapper.readValue(request.bytes(), entityClass);
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
  public void postJsonOverTor(String urlStr, Object bodyObj) throws HttpException {
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
      request.contentType(HttpRequest.CONTENT_TYPE_JSON).send(jsonBody.getBytes());
      execute(request);
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
  public void postUrlEncoded(String urlStr, Map<String, String> body) throws HttpException {
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
      request.contentType(HttpRequest.CONTENT_TYPE_FORM).send(bodyUrlEncoded.getBytes());
      execute(request);
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

  private void execute(HttpRequest request) throws HttpException {
    if (!request.header(HttpRequest.HEADER_USER_AGENT, ClientUtils.USER_AGENT).ok()) {
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
