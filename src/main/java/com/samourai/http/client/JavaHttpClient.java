package com.samourai.http.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class JavaHttpClient implements IHttpClient {
  @Override
  public <T> T parseJson(String url, Class<T> entityClass) throws HttpException {
    RestTemplate restTemplate = new RestTemplate();
    try {
      ResponseEntity<T> result = restTemplate.getForEntity(url, entityClass);
      if (result == null || !result.getStatusCode().is2xxSuccessful()) {
        // response error
        String responseBody = null;
        throw new HttpException(new Exception("unable to retrieve pools"), responseBody);
      }
      return result.getBody();
    } catch (RestClientResponseException e) {
      String responseBody = e.getResponseBodyAsString();
      throw new HttpException(e, responseBody);
    }
  }

  @Override
  public void postJsonOverTor(String url, Object body) throws HttpException {
    try {
      // TODO use TOR
      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity result = restTemplate.postForEntity(url, body, null);
      if (result == null || !result.getStatusCode().is2xxSuccessful()) {
        // response error
        String responseBody = null;
        throw new HttpException(new Exception("statusCode not successful"), responseBody);
      }
    } catch (RestClientResponseException e) {
      String responseBody = e.getResponseBodyAsString();
      throw new HttpException(e, responseBody);
    }
  }
}
