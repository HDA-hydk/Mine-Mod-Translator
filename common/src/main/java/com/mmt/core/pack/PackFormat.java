package com.mmt.core.pack;

import com.mmt.core.log.MmtLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackFormat {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(\\.(\\d+))?");

    public static int getPackFormat(String mcVersion, MmtLogger logger) {
        if (mcVersion == null || mcVersion.isEmpty()) {
            logger.warn("MC version not provided, using default pack_format 46");
            return 46;
        }

        Matcher matcher = VERSION_PATTERN.matcher(mcVersion);
        if (!matcher.find()) {
            logger.warn("Failed to parse MC version: " + mcVersion + ", using default pack_format 46");
            return 46;
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;

            if (major == 1 && minor == 20) {
                if (patch == 1) return 15;
                if (patch >= 2 && patch <= 4) return 18;
                if (patch >= 5) return 22;
            } else if (major == 1 && minor == 21) {
                if (patch <= 1) return 34;
                if (patch >= 2 && patch <= 3) return 42;
                if (patch >= 4) return 46;
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse MC version: " + mcVersion + ", using default pack_format 46");
        }

        logger.warn("Unknown MC version: " + mcVersion + ", using default pack_format 46");
        return 46;
    }
}