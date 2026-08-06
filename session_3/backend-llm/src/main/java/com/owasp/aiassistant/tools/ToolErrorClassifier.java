package com.owasp.aiassistant.tools;

import com.owasp.aiassistant.exception.ToolConnectivityException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

public final class ToolErrorClassifier {

    private ToolErrorClassifier() {
    }

    public static boolean isConnectivityError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ToolConnectivityException
                    || current instanceof SSLException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof SocketTimeoutException
                    || current instanceof TimeoutException
                    || current instanceof WebClientRequestException
                    || isPkixMessage(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static String formatWarning(String toolName, Throwable throwable) {
        Throwable root = rootCause(throwable);
        String detail = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
        return toolName + " didn't work because " + detail;
    }

    public static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    public static boolean isResponseError(Throwable throwable) {
        return rootCause(throwable) instanceof WebClientResponseException;
    }

    private static boolean isPkixMessage(String message) {
        return message != null && message.contains("PKIX path building failed");
    }
}
