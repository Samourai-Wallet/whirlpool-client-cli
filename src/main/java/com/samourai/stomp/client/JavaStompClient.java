package com.samourai.stomp.client;

import com.samourai.whirlpool.cli.config.CliConfig;
import com.samourai.whirlpool.cli.services.CliTorClientService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.utils.ClientUtils;
import com.samourai.whirlpool.client.utils.MessageErrorListener;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.JettyXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

public class JavaStompClient implements IStompClient {
  private static final Logger log = LoggerFactory.getLogger(JavaStompClient.class);
  private static final int HEARTBEAT_DELAY = 20000;

  private CliTorClientService torClientService;
  private CliConfig cliConfig;

  private WebSocketStompClient stompClient;
  private StompSession stompSession;

  public JavaStompClient(CliTorClientService torClientService, CliConfig cliConfig) {
    this.torClientService = torClientService;
    this.cliConfig = cliConfig;
  }

  @Override
  public void connect(
      String url,
      Map<String, String> stompHeaders,
      MessageErrorListener<IStompMessage, Throwable> onConnectOnDisconnectListener) {

    WebSocketHttpHeaders httpHeaders = computeHttpHeaders();
    StompHeaders stompHeadersObj = computeStompHeaders(stompHeaders);
    try {
      this.stompClient = computeStompClient();
      this.stompSession =
          stompClient // set stompSession twice, as we need it for getSessionId()
              .connect(
                  url,
                  httpHeaders,
                  stompHeadersObj,
                  computeStompSessionHandler(onConnectOnDisconnectListener))
              .get();
    } catch (Exception e) {
      // connexion failed
      disconnect();
      onConnectOnDisconnectListener.onError(e);
    }
  }

  @Override
  public String getSessionId() {
    return stompSession.getSessionId();
  }

  @Override
  public void subscribe(
      Map<String, String> stompHeaders,
      final MessageErrorListener<IStompMessage, String> onMessageOnErrorListener) {
    StompHeaders stompHeadersObj = computeStompHeaders(stompHeaders);
    JavaStompFrameHandler frameHandler = new JavaStompFrameHandler(onMessageOnErrorListener);
    stompSession.subscribe(stompHeadersObj, frameHandler);
  }

  @Override
  public void send(Map<String, String> stompHeaders, Object payload) {
    StompHeaders stompHeadersObj = computeStompHeaders(stompHeaders);
    stompSession.send(stompHeadersObj, payload);
  }

  @Override
  public void disconnect() {
    if (stompSession != null) {
      try {
        stompSession.disconnect();
      } catch (Exception e) {
      }
      stompSession = null;
    }

    if (stompClient != null) {
      try {
        stompClient.stop();
      } catch (Exception e) {
      }
      stompClient = null;
    }
  }

  private StompSessionHandlerAdapter computeStompSessionHandler(
      final MessageErrorListener<IStompMessage, Throwable> onConnectOnDisconnectListener) {
    return new StompSessionHandlerAdapter() {

      @Override
      public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        super.afterConnected(session, connectedHeaders);
        if (log.isDebugEnabled()) {
          log.debug(
              "connected, connectedHeaders=" + connectedHeaders + ", stompSession=" + stompSession);
        }
        // set session twice, as we need it for subscribe
        stompSession = session;
        // send back connected headers through IStompMessage
        IStompMessage stompMessage = new JavaStompMessage(connectedHeaders, null);
        onConnectOnDisconnectListener.onMessage(stompMessage);
      }

      @Override
      public void handleException(
          StompSession session,
          StompCommand command,
          StompHeaders headers,
          byte[] payload,
          Throwable exception) {
        super.handleException(session, command, headers, payload, exception);
        log.error(" ! transportException", exception);
      }

      @Override
      public void handleTransportError(StompSession session, Throwable exception) {
        super.handleTransportError(session, exception);
        log.error(
            " ! transportError: " + exception.getClass().getName() + ": " + exception.getMessage());
        disconnect();
        onConnectOnDisconnectListener.onError(exception);

        if (log.isDebugEnabled()) {
          log.error("", exception);
        }
      }
    };
  }

  private WebSocketStompClient computeStompClient() throws Exception {
    // enable heartbeat (mandatory to detect client disconnect)
    ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
    te.setPoolSize(1);
    te.setThreadNamePrefix("wss-heartbeat-thread-");
    te.initialize();

    SockJsClient webSocketClient = computeWebSocketClient();
    WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    stompClient.setTaskScheduler(te);
    stompClient.setDefaultHeartbeat(new long[] {HEARTBEAT_DELAY, HEARTBEAT_DELAY});
    return stompClient;
  }

  private HttpClient computeHttpClient() throws NotifiableException {
    return CliUtils.computeHttpClient(false, torClientService, cliConfig.getCliProxy());
  }

  private SockJsClient computeWebSocketClient() throws Exception {
    HttpClient jettyHttpClient = computeHttpClient();

    if (log.isDebugEnabled()) {
      log.debug("Using websocket transports: Websocket, XHR");
    }
    JettyWebSocketClient jettyWebSocketClient =
        new JettyWebSocketClient(new WebSocketClient(jettyHttpClient));
    List<Transport> webSocketTransports =
        Arrays.asList(
            new WebSocketTransport(jettyWebSocketClient), new JettyXhrTransport(jettyHttpClient));

    SockJsClient sockJsClient = new SockJsClient(webSocketTransports);
    jettyHttpClient.start();
    jettyWebSocketClient.start();
    return sockJsClient;
  }

  private WebSocketHttpHeaders computeHttpHeaders() {
    WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
    httpHeaders.set("user-agent", ClientUtils.USER_AGENT + "!!"); // prevent user-agent tracking
    return httpHeaders;
  }

  private StompHeaders computeStompHeaders(Map<String, String> stompHeaders) {
    StompHeaders stompHeadersObj = new StompHeaders();
    for (Map.Entry<String, String> entry : stompHeaders.entrySet()) {
      stompHeadersObj.set(entry.getKey(), entry.getValue());
    }
    return stompHeadersObj;
  }
}
