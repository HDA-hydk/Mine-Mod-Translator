package com.mmt.core.translate.api;

import com.mmt.core.log.MmtLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtil {
    private static final int CONNECT_TIMEOUT_MS = 10000;

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
        HttpURLConnection conn = null;
        try {
            URL apiUrl = new URL(url);
            conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(timeoutSeconds * 1000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int statusCode = conn.getResponseCode();

            String responseBody;
            InputStream stream = (statusCode >= 400) ? conn.getErrorStream() : conn.getInputStream();
            responseBody = readStream(stream);

            if (statusCode == 200) {
                return responseBody;
            } else if (statusCode == 429) {
                throw new ApiException("Rate limited (429)", ApiException.Type.RATE_LIMITED);
            } else if (statusCode >= 500) {
                throw new ApiException("Server error (" + statusCode + ")", ApiException.Type.SERVER_ERROR);
            } else if (statusCode >= 400) {
                throw new ApiException("Client error (" + statusCode + "): " + responseBody, ApiException.Type.CLIENT_ERROR);
            } else {
                throw new ApiException("Unexpected status code: " + statusCode, ApiException.Type.UNKNOWN);
            }
        } catch (SocketTimeoutException e) {
            throw new ApiException("Request timed out", ApiException.Type.TIMEOUT, e);
        } catch (IOException e) {
            throw new ApiException("Network error", ApiException.Type.NETWORK_ERROR, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            is.close();
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
