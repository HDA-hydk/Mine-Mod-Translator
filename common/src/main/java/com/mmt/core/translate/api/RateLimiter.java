package com.mmt.core.translate.api;

import com.mmt.core.log.MmtLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final MmtLogger logger;
    private final int intervalMs;
    private final int maxRetries;
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();

    public RateLimiter(MmtLogger logger, int intervalMs, int maxRetries) {
        this.logger = logger;
        this.intervalMs = intervalMs;
        this.maxRetries = maxRetries;
    }

    public String executeWithRetry(ApiCall call) throws HttpUtil.ApiException {
        String key = call.getKey();
        int retries = 0;

        while (retries <= maxRetries) {
            try {
                waitIfNeeded(key);

                String result = call.execute();
                lastRequestTimes.put(key, System.currentTimeMillis());
                return result;
            } catch (HttpUtil.ApiException e) {
                retries++;

                if (!e.shouldRetry() || retries > maxRetries) {
                    throw e;
                }

                long waitTime = (long) Math.pow(2, retries - 1) * 1000;
                logger.warn("API call failed, retrying in " + waitTime + "ms (attempt " + retries + "/" + maxRetries + "): " + e.getMessage());

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new HttpUtil.ApiException("Interrupted during retry wait", HttpUtil.ApiException.Type.NETWORK_ERROR, ie);
                }
            }
        }

        throw new HttpUtil.ApiException("Max retries exceeded", HttpUtil.ApiException.Type.UNKNOWN);
    }

    private void waitIfNeeded(String key) {
        if (intervalMs <= 0) {
            return;
        }

        Long lastTime = lastRequestTimes.get(key);
        if (lastTime != null) {
            long elapsed = System.currentTimeMillis() - lastTime;
            if (elapsed < intervalMs) {
                try {
                    Thread.sleep(intervalMs - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public interface ApiCall {
        String execute() throws HttpUtil.ApiException;
        String getKey();
    }
}