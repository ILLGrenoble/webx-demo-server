package eu.ill.webxdemo.ws;

import eu.ill.webx.WebXClientConfiguration;
import eu.ill.webx.WebXEngineConfiguration;
import eu.ill.webx.WebXHostConfiguration;
import eu.ill.webx.WebXTunnel;
import eu.ill.webx.exceptions.WebXConnectionException;
import eu.ill.webxdemo.Configuration;
import eu.ill.webxdemo.model.Credentials;
import eu.ill.webxdemo.services.AuthService;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class WebSocketTunnelListener implements WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketTunnelListener.class);
    private static final String WEBX_HOST_PARAM = "webxhost";
    private static final String WEBX_PORT_PARAM = "webxport";
    private static final String WEBX_SESSION_ID_PARAM = "sessionid";
    private static final String TOKEN_PARAM = "token";
    private static final String WIDTH_PARAM = "width";
    private static final String HEIGHT_PARAM = "height";
    private static final String KEYBOARD_PARAM = "keyboard";

    private final Configuration configuration;

    private ConnectionThread connectionThread;

    public WebSocketTunnelListener(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onWebSocketConnect(final Session session) {

        WebXClientConfiguration clientConfiguration;
        WebXEngineConfiguration engineConfiguration = null;
        WebXHostConfiguration webXConfiguration;

        if (this.configuration.getStandaloneHost() != null && this.configuration.getStandalonePort() != null) {
            String hostname = this.configuration.getStandaloneHost();
            Integer port = this.configuration.getStandalonePort();
            webXConfiguration = new WebXHostConfiguration(hostname, port, true);

            clientConfiguration = WebXClientConfiguration.ForStandaloneSession();

        } else {
            Map<String, List<String>> params = session.getUpgradeRequest().getParameterMap();

            // Get all the other params
            Integer port = this.getIntegerParam(params, WEBX_PORT_PARAM);
            String hostname = this.getStringParam(params, WEBX_HOST_PARAM);
            webXConfiguration = new WebXHostConfiguration(hostname, port, false);


            String sessionId = this.getStringParam(params, WEBX_SESSION_ID_PARAM);
            if (sessionId != null) {
                if (sessionId.length() != 32) {
                    logger.error("SessionId {} is invalid", sessionId);
                    session.close();
                    return;
                }
                clientConfiguration = WebXClientConfiguration.ForExistingSession(sessionId);

            } else {
                String token = this.getStringParam(params, TOKEN_PARAM);
                Credentials credentials = AuthService.instance().getCredentials(token);
                if (!credentials.isValid()) {
                    logger.warn("Connection credentials are invalid. Disconnecting");
                    session.close();
                    return;
                }
                String username = credentials.getUsername();
                String password = credentials.getPassword();

                Integer width = this.getIntegerParam(params, WIDTH_PARAM);
                Integer height = this.getIntegerParam(params, HEIGHT_PARAM);
                String keyboard = this.getStringParam(params, KEYBOARD_PARAM);

                clientConfiguration = WebXClientConfiguration.ForLogin(
                        username,
                        password,
                        width != null ? width : configuration.getDefaultScreenWidth(),
                        height != null ? height : configuration.getDefaultScreenHeight(),
                        keyboard != null ? keyboard : configuration.getDefaultKeyboardLayout());

                engineConfiguration = new WebXEngineConfiguration();
                engineConfiguration.setParameter("logLevel", "debug");
                engineConfiguration.setParameter("runtimeMaxQualityIndex", "12");
            }
        }

        try {
            // Connect to host
            WebXTunnel tunnel = WebXTunnel.Connect(webXConfiguration, clientConfiguration, engineConfiguration);

            // Create thread to read from tunnel
            this.connectionThread = new ConnectionThread(tunnel, session);
            connectionThread.start();

        } catch (WebXConnectionException exception) {
            logger.error("Failed to connect to WebX server. Client not created: {}", exception.getMessage());
            session.close();
        }

    }

    @Override
    public void onWebSocketText(String message) {
        throw new UnsupportedOperationException("Text WebSocket messages are not supported.");
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        if (this.connectionThread != null && this.connectionThread.isConnected()) {
            this.connectionThread.write(payload);

        } else {
            logger.error("Received instruction on closed client");
        }

    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        logger.debug("WebSocket tunnel closing due to error", throwable);

        this.disconnect();
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.debug("WebSocket closing{}", reason != null ? " with reason " + reason : "");

        this.disconnect();
    }

    private void disconnect() {
        if (this.connectionThread != null) {
            this.connectionThread.closeTunnel();
            try {
                this.connectionThread.join();

            } catch (InterruptedException e) {
                logger.error("Failed to join the connection thread");
            }
            this.connectionThread = null;
        }
    }

    private String getStringParam(Map<String, List<String>> params, String paramName) {
        return params.containsKey(paramName) ? params.get(paramName).get(0) : null;
    }

    private Integer getIntegerParam(Map<String, List<String>> params, String paramName) {
        Integer param = null;
        String paramString = params.containsKey(paramName) ? params.get(paramName).get(0) : null;
        if (paramString != null) {
            try {
                param = Integer.parseInt(paramString);

            } catch (NumberFormatException ignore) {
                logger.warn("Unable to parse integer {} parameter (\"{}\")", paramName, paramString);
            }
        }
        return param;
    }

}
