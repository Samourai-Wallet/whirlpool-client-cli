package com.samourai.http.client;

import com.samourai.wallet.api.backend.beans.HttpException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public abstract class JavaHttpClient extends JacksonHttpClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private HttpClient httpClientShared;
  private HttpClient httpClientRegOut;
  private long requestTimeout;

  public JavaHttpClient(long requestTimeout) {
    super();
    httpClientShared = null;
    httpClientRegOut = null;
    this.requestTimeout = requestTimeout;
  }

  protected abstract HttpClient computeHttpClient(boolean isRegisterOutput) throws Exception;

  @Override
  protected String requestJsonGet(String urlStr, Map<String, String> headers) throws Exception {
    Request req = computeHttpRequest(false, urlStr, HttpMethod.GET, headers);
    return requestJson(req);
  }

  @Override
  protected String requestJsonPost(String urlStr, Map<String, String> headers, String jsonBody)
      throws Exception {
    Request req = computeHttpRequest(false, urlStr, HttpMethod.POST, headers);
    req.content(
        new StringContentProvider(
            MediaType.APPLICATION_JSON_VALUE, jsonBody, StandardCharsets.UTF_8));
    return requestJson(req);
  }

  @Override
  protected String requestJsonPostOverTor(
      String urlStr, Map<String, String> headers, String jsonBody) throws Exception {
    Request req = computeHttpRequest(true, urlStr, HttpMethod.POST, headers);
    req.content(
        new StringContentProvider(
            MediaType.APPLICATION_JSON_VALUE, jsonBody, StandardCharsets.UTF_8));
    return requestJson(req);
  }

  @Override
  protected String requestJsonPostUrlEncoded(
      String urlStr, Map<String, String> headers, Map<String, String> body) throws Exception {
    Request req = computeHttpRequest(false, urlStr, HttpMethod.POST, headers);
    req.content(new FormContentProvider(computeBodyFields(body)));
    return requestJson(req);
  }

  private Fields computeBodyFields(Map<String, String> body) {
    Fields fields = new Fields();
    for (Map.Entry<String, String> entry : body.entrySet()) {
      fields.put(entry.getKey(), entry.getValue());
    }
    return fields;
  }

  private String requestJson(Request req) throws Exception {
    ContentResponse response = req.send();
    if (response.getStatus() != HttpStatus.OK_200) {
      String responseBody = response.getContentAsString();
      log.error(
          "Http query failed: status=" + response.getStatus() + ", responseBody=" + responseBody);
      throw new HttpException(
          new Exception("Http query failed: status=" + response.getStatus()), responseBody);
    }
    String responseContent = response.getContentAsString();
    return responseContent;
  }

  public HttpClient getHttpClient(boolean isRegisterOutput) throws Exception {
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

  private Request computeHttpRequest(
      boolean isRegOut, String url, HttpMethod method, Map<String, String> headers)
      throws Exception {
    if (log.isDebugEnabled()) {
      String headersStr = headers != null ? " (" + headers.keySet() + ")" : "";
      log.debug("+" + method + ": " + url + headersStr);
    }
    HttpClient httpClient = getHttpClient(isRegOut);
    Request req = httpClient.newRequest(url);
    req.method(method);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        req.header(entry.getKey(), entry.getValue());
      }
    }
    req.timeout(requestTimeout, TimeUnit.MILLISECONDS);
    return req;
  }

  @Override
  protected void onRequestError(Exception e, boolean isRegisterOutput) {
    super.onRequestError(e, isRegisterOutput);
    if (!isRegisterOutput) {
      if (httpClientShared != null) {
        try {
          httpClientShared.stop();
        } catch (Exception ee) {
        }
        if (log.isDebugEnabled()) {
          log.debug("--httpClientShared");
        }
        httpClientShared = null;
      }
    } else {
      if (httpClientRegOut != null) {
        try {
          httpClientRegOut.stop();
        } catch (Exception ee) {
        }
        if (log.isDebugEnabled()) {
          log.debug("--httpClientRegOut");
        }
        httpClientRegOut = null;
      }
    }
  }
}
