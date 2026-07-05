package com.mmt.core.translate.api;

import com.mmt.core.config.ConfigManager;

public class ApiConfig {
    private final String apiUrl;
    private final String apiKey;
    private final String apiModel;
    private final int requestCharLimit;
    private final int requestIntervalMs;
    private final int retryCount;

    public ApiConfig(String urlKey, String keyKey, String modelKey, ConfigManager configManager) {
        this.apiUrl = configManager.getString(urlKey, "");
        this.apiKey = configManager.getString(keyKey, "");
        this.apiModel = configManager.getString(modelKey, "");
        this.requestCharLimit = configManager.getInt("request_char_limit", 8000);
        this.requestIntervalMs = configManager.getInt("request_interval_ms", 1000);
        this.retryCount = configManager.getInt("retry_count", 3);
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiModel() {
        return apiModel;
    }

    public int getRequestCharLimit() {
        return requestCharLimit;
    }

    public int getRequestIntervalMs() {
        return requestIntervalMs;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public boolean isValid() {
        return apiUrl != null && !apiUrl.isEmpty() && apiKey != null && !apiKey.isEmpty();
    }
}