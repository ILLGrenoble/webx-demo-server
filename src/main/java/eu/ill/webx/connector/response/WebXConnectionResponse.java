package eu.ill.webx.connector.response;

import eu.ill.webx.domain.utils.Size;

public class WebXConnectionResponse extends WebXResponse {

    private int publisherPort;
    private int collectorPort;
    private Size screenSize;

    public WebXConnectionResponse() {
    }

    public int getPublisherPort() {
        return publisherPort;
    }

    public void setPublisherPort(int publisherPort) {
        this.publisherPort = publisherPort;
    }

    public int getCollectorPort() {
        return collectorPort;
    }

    public void setCollectorPort(int collectorPort) {
        this.collectorPort = collectorPort;
    }

    public Size getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(Size screenSize) {
        this.screenSize = screenSize;
    }
}