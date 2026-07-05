package com.mmt.core.translate.api;

import com.mmt.core.log.MmtLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpUtil {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final MmtLogger logger;
    private final int timeoutSeconds;

    public HttpUtil(MmtLogger logger) {
        this(logger, 30);
    }

    public HttpUtil(MmtLogger logger, int timeoutSeconds) {
        this.logger = logger;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String postJson(String url, String jsonBody, Map<String, String> headers) throws ApiException {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json");

            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode == 200) {
                return response.body();
            } else if (statusCode == 429) {
                throw new ApiException("Rate limited (429)", ApiException.Type.RATE_LIMITED);
            } else if (statusCode >= 500) {
                throw new ApiException("Server error (" + statusCode + ")", ApiException.Type.SERVER_ERROR);
            } else if (statusCode >= 400) {
                throw new ApiException("Client error (" + statusCode + "): " + response.body(), ApiException.Type.CLIENT_ERROR);
            } else {
                throw new ApiException("Unexpected status code: " + statusCode, ApiException.Type.UNKNOWN);
            }
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ApiException("Request timed out", ApiException.Type.TIMEOUT, e);
        } catch (IOException | InterruptedException e) {
            throw new ApiException("Network error", ApiException.Type.NETWORK_ERROR, e);
        }
    }

    public static class ApiException extends Exception {
        private final Type type;

        public enum Type {
            TIMEOUT,
            NETWORK_ERROR,
            RATE_LIMITED,
            SERVER_ERROR,
            CLIENT_ERROR,
            UNKNOWN
        }

        public ApiException(String message, Type type) {
            super(message);
            this.type = type;
        }

        public ApiException(String message, Type type, Throwable cause) {
            super(message, cause);
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        public boolean shouldRetry() {
            return type == Type.TIMEOUT || type == Type.NETWORK_ERROR ||
                   type == Type.SERVER_ERROR || type == Type.RATE_LIMITED;
        }
    }
}