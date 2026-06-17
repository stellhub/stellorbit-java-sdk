package io.github.stellorbit.client;

public class StellorbitClientException extends RuntimeException {

    public StellorbitClientException(String message) {
        super(message);
    }

    public StellorbitClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
