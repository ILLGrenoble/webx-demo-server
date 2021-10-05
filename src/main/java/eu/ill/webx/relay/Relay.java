package eu.ill.webx.relay;

import eu.ill.webx.connector.WebXConnector;
import eu.ill.webx.connector.WebXMessageListener;
import eu.ill.webx.transport.serializer.Serializer;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;

public class Relay implements WebXMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(Relay.class);
    private final WebXConnector connector;
    private Serializer serializer;

    private Thread webXListenerThread;
    private Thread clientCommandThread;
    private LinkedBlockingDeque<byte[]> messageQueue = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<byte[]> instructionQueue = new LinkedBlockingDeque<>();
    private boolean running = false;

    private Session session;
    private RemoteEndpoint remoteEndpoint;
    BinaryEndpointCommunicator binaryEndpointCommunicator = null;
    StringEndpointCommunicator stringEndpointCommunicator = null;
    EndpointCommunicator activeEndpointCommunicator = null;


    public Relay(Session session, WebXConnector connector) {
        this.connector = connector;
        this.serializer = connector.getSerializer();
        if (session != null) {
            this.session = session;
            this.remoteEndpoint = session.getRemote();
            this.binaryEndpointCommunicator = new BinaryEndpointCommunicator(this.remoteEndpoint);
            this.stringEndpointCommunicator = new StringEndpointCommunicator(this.remoteEndpoint);
            boolean useBinary = this.connector.getSerializer().getType().equals("binary");
            if (useBinary) {
                this.activeEndpointCommunicator = this.binaryEndpointCommunicator;

            } else {
                this.activeEndpointCommunicator = this.stringEndpointCommunicator;
            }
        }
    }

    public Thread getWebXListenerThread() {
        return webXListenerThread;
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void start() {
        if (!running) {
            running = true;

            this.webXListenerThread = new Thread(() -> this.webXListenerLoop());
            this.webXListenerThread.start();

            this.clientCommandThread = new Thread(() -> this.clientCommandLoop());
            this.clientCommandThread.start();
        }
    }

    public synchronized void stop() {
        if (running) {
            try {
                running = false;
                if (this.webXListenerThread != null) {
                    this.webXListenerThread.interrupt();
                    this.webXListenerThread.join();
                    this.webXListenerThread = null;
                }

                if (this.clientCommandThread != null) {
                    this.clientCommandThread.interrupt();
                    this.clientCommandThread.join();
                    this.clientCommandThread = null;
                }

            } catch (InterruptedException e) {
                logger.error("Stop of relay message listener and client command threads interrupted");
            }
        }
    }

    @Override
    public void onMessage(byte[] messageData) {
        try {
            this.messageQueue.put(messageData);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding message to relay message queue");
        }
    }

    public void queueCommand(byte[] commandData) {
        try {
            this.instructionQueue.put(commandData);

        } catch (InterruptedException e) {
            logger.error("Interrupted when adding command to relay command queue");
        }
    }

    private void webXListenerLoop() {
        while (this.running) {
            try {
                byte[] messageData = this.messageQueue.take();
                this.activeEndpointCommunicator.sendData(messageData);

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }

    private void clientCommandLoop() {
        while (this.running) {
            try {
                byte[] instructionData = this.instructionQueue.take();

                if (instructionData.length == 4) {
                    String messageString = new String(instructionData);
                    if (messageString.equals("comm")) {
                        String serializerType = this.connector.getSerializer().getType();
                        this.stringEndpointCommunicator.sendData(serializerType);
                    }

                } else {
                    byte[] responseData = this.connector.sendRequestData(instructionData);
                    this.activeEndpointCommunicator.sendData(responseData);
                }

            } catch (InterruptedException ie) {
                logger.info("Relay message listener thread interrupted");
            }
        }
    }
}
