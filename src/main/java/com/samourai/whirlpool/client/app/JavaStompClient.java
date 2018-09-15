package com.samourai.whirlpool.client.app;

import com.samourai.whirlpool.client.Application;
import com.samourai.whirlpool.client.mix.transport.IWhirlpoolStompClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.websocket.MessageHandler;
import java.util.Map;

public class JavaStompClient implements IWhirlpoolStompClient {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final String HEADER_USERNAME = "user-name";

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String stompSessionId;

    @Override
    public void connect(String url, Map<String, String> stompHeaders, MessageHandler.Whole<String> onConnect, MessageHandler.Whole<Throwable> onDisconnect) {
        this.stompClient = computeWebSocketClient();

        StompHeaders stompHeadersObj = computeStompHeaders(stompHeaders);
        try {
            this.stompSession = stompClient.connect(url, (WebSocketHttpHeaders) null, stompHeadersObj, computeStompSessionHandler(onConnect, onDisconnect)).get();
            this.stompSessionId = stompSession.getSessionId();
        } catch(Exception e) {
            disconnect();
            onDisconnect.onMessage(e);
        }
    }

    @Override
    public String getSessionId() {
        return stompSessionId;
    }

    @Override
    public void subscribe(Map<String, String> stompHeaders, final MessageHandler.Whole<Object> onMessage, final MessageHandler.Whole<String> onError) {
        StompHeaders stompHeadersObj = computeStompHeaders(stompHeaders);
        JavaStompFrameHandler frameHandler = new JavaStompFrameHandler(onMessage, onError);
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
                log.error("", e);
            }
        }

        if (stompClient != null) {
            try {
                stompClient.stop();
            } catch(Exception e) {
                log.error("", e);
            }
            stompClient = null;
        }
    }

    private StompSessionHandlerAdapter computeStompSessionHandler(final MessageHandler.Whole<String> onConnect, final MessageHandler.Whole<Throwable> onDisconnect) {
        return new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                super.afterConnected(session, connectedHeaders);

                // username
                String stompUsername = connectedHeaders.get(HEADER_USERNAME).iterator().next();
                onConnect.onMessage(stompUsername);
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                super.handleException(session, command, headers, payload, exception);
                log.error(" ! transportException",exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                super.handleTransportError(session, exception);
                if (exception instanceof ConnectionLostException) {
                    disconnect();
                    onDisconnect.onMessage(exception);
                }
                else {
                    log.error(" ! transportError : " + exception.getMessage());
                }
            }
        };
    }

    private WebSocketStompClient computeWebSocketClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient;
    }

    private StompHeaders computeStompHeaders(Map<String, String> stompHeaders) {
        StompHeaders stompHeadersObj = new StompHeaders();
        for (Map.Entry<String,String> entry : stompHeaders.entrySet()) {
            stompHeadersObj.set(entry.getKey(), entry.getValue());
        }
        return stompHeadersObj;
    }
}
