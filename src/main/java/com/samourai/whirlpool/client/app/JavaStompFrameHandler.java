package com.samourai.whirlpool.client.app;

import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompHeaders;

import javax.websocket.MessageHandler;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;

public class JavaStompFrameHandler implements org.springframework.messaging.simp.stomp.StompFrameHandler {
    private Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final MessageHandler.Whole<Object> frameHandler;
    private final MessageHandler.Whole<String> errorHandler;

    public JavaStompFrameHandler(MessageHandler.Whole<Object> frameHandler, MessageHandler.Whole<String> errorHandler) {
        this.frameHandler = frameHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        String messageType = headers.get(WhirlpoolProtocol.HEADER_MESSAGE_TYPE).get(0);
        try {
            return Class.forName(messageType);
        }
        catch(ClassNotFoundException e) {
            log.error("unknown message type: " + messageType, e);
            return null;
        }
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        // check protocol version
        String protocolVersion = headers.getFirst(WhirlpoolProtocol.HEADER_PROTOCOL_VERSION);
        if (!WhirlpoolProtocol.PROTOCOL_VERSION.equals(protocolVersion)) {
            String errorMessage = "Version mismatch: server=" + (protocolVersion != null ? protocolVersion : "unknown") + ", client=" + WhirlpoolProtocol.PROTOCOL_VERSION;
            errorHandler.onMessage(errorMessage);
            return;
        }

        // unserialize payload: already deserialized by StompFrameHandler
        frameHandler.onMessage(payload);
    }
}
