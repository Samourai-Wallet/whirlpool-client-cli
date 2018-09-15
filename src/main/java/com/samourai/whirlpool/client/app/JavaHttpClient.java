package com.samourai.whirlpool.client.app;

import com.samourai.whirlpool.client.whirlpool.httpClient.IWhirlpoolHttpClient;
import com.samourai.whirlpool.client.whirlpool.httpClient.WhirlpoolHttpException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

public class JavaHttpClient implements IWhirlpoolHttpClient {
    @Override
    public <T> T getJsonAsEntity(String url, Class<T> entityClass) throws WhirlpoolHttpException {
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<T> result = restTemplate.getForEntity(url, entityClass);
            if (result == null || !result.getStatusCode().is2xxSuccessful()) {
                // response error
                String responseBody = null;
                throw new WhirlpoolHttpException(new Exception("unable to retrieve pools"), responseBody);
            }
            return result.getBody();
        } catch(RestClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            throw new WhirlpoolHttpException(e, responseBody);
        }
    }

    @Override
    public void postJsonOverTor(String url, Object body) throws WhirlpoolHttpException {
        try {
            // TODO use TOR
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity result = restTemplate.postForEntity(url, body, null);
            if (result == null || !result.getStatusCode().is2xxSuccessful()) {
                // response error
                String responseBody = null;
                throw new WhirlpoolHttpException(new Exception("statusCode not successful"), responseBody);
            }
        }
        catch(RestClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            throw new WhirlpoolHttpException(e, responseBody);
        }
    }
}
