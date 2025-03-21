package eu.ill.webxdemo.ws;

import eu.ill.webx.WebXTunnel;
import eu.ill.webx.exceptions.WebXClientException;
import eu.ill.webx.exceptions.WebXConnectionInterruptException;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ConnectionThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionThread.class);

    private final WebXTunnel tunnel;
    private final Session session;

    public ConnectionThread(final WebXTunnel tunnel, final Session session) {
        this.tunnel = tunnel;
        this.session = session;
    }

    public void write(byte[] data) {
        try {
            this.tunnel.write(data);

        } catch (WebXClientException exception) {
            logger.debug("Connection to WebX server is closed", exception);
        }
    }

    public void run() {

        try {
            byte[] messageData;
            while (tunnel.isConnected() && (messageData = tunnel.read()) != null) {
                this.sendData(messageData);
            }

        } catch (WebXClientException exception) {
            logger.error("WebSocket connection terminated due to client error {}", exception.getMessage());

        } catch (WebXConnectionInterruptException exception) {
            logger.error("WebSocket connection terminated due to interruption {}", exception.getMessage());
        }

        this.session.close();
    }

    public void closeTunnel() {
        this.tunnel.disconnect();
    }

    public boolean isConnected() {
        return this.tunnel.isConnected();
    }

    private void sendData(byte[] data) {
        try {
            this.session.getRemote().sendBytes(ByteBuffer.wrap(data));

        } catch (IOException exception) {
            logger.error("Failed to write binary data to web socket", exception);
            this.closeTunnel();
        }
    }
}
