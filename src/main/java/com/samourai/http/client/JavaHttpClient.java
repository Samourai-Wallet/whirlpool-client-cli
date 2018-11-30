package com.samourai.http.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.samourai.tor.client.JavaTorClient;
import com.samourai.tor.client.JavaTorConnexion;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaHttpClient implements IHttpClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Optional<JavaTorClient> torClient;
  private ObjectMapper objectMapper;

  public JavaHttpClient(Optional<JavaTorClient> torClient) {
    this.torClient = torClient;
    this.objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Override
  public <T> T parseJson(String urlStr, Class<T> entityClass) throws HttpException {
    try {
      HttpRequest request;
      if (torClient.isPresent()) {
        // use TOR - same circuit for all GET requests
        JavaTorConnexion sharedTorConnexion = torClient.get().getConnexion(false);
        URL url = sharedTorConnexion.getUrl(urlStr);
        request = HttpRequest.get(url);
      } else {
        // standard connexion
        request = HttpRequest.get(urlStr);
      }
      checkResponseSuccess(request);
      T result = objectMapper.readValue(request.bytes(), entityClass);
      // keep sharedTorConnexion open
      return result;
    } catch (Exception e) {
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      throw (HttpException) e;
    }
  }

  @Override
  public void postJsonOverTor(String urlStr, Object bodyObj) throws HttpException {
    JavaTorConnexion privateTorConnexion = null;
    try {
      String jsonBody = objectMapper.writeValueAsString(bodyObj);
      HttpRequest request;
      if (torClient.isPresent()) {
        // different circuit for each POST request
        privateTorConnexion = torClient.get().getConnexion(true);
        URL url = privateTorConnexion.getUrl(urlStr);
        request = HttpRequest.post(url).header("Content-Type", "application/json");
      } else {
        // standard connexion
        request = HttpRequest.post(urlStr).header("Content-Type", "application/json");
      }
      request.send(jsonBody.getBytes());
      checkResponseSuccess(request);
      if (privateTorConnexion != null) {
        privateTorConnexion.close();
      }
    } catch (Exception e) {
      if (!(e instanceof HttpException)) {
        e = new HttpException(e, null);
      }
      if (privateTorConnexion != null) {
        privateTorConnexion.close();
      }
      throw (HttpException) e;
    }
  }

  private void checkResponseSuccess(HttpRequest request) throws HttpException {
    if (!request.ok()) {
      throw new HttpException(new Exception("statusCode=" + request.code()), request.body());
    }
  }
}
