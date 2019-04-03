package eu.ill.webx.connector;

import com.fasterxml.jackson.annotation.JsonValue;

public class WebXRequest {

    public enum Type {
        Connect(1),
        Image(2),
        Images(3),
        Window(4),
        Windows(5),
        Mouse(6);

        private final int value;
        private Type(int value) {
            this.value = value;
        }

        @JsonValue
        public int getValue() {
            return this.value;
        }
    }

    private Type type;
    private String stringPayload;
    private int integerPayload;

    public WebXRequest(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public String getStringPayload() {
        return stringPayload;
    }

    public void setStringPayload(String stringPayload) {
        this.stringPayload = stringPayload;
    }

    public int getIntegerPayload() {
        return integerPayload;
    }

    public void setIntegerPayload(int integerPayload) {
        this.integerPayload = integerPayload;
    }
}