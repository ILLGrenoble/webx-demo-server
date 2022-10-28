package eu.ill.webx.exceptions;

public class WebXClientException extends Exception{

    public WebXClientException() {
    }

    public WebXClientException(String message) {
        super(message);
    }

    public WebXClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
