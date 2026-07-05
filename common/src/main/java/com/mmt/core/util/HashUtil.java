package com.mmt.core.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

/**
 * 哈希工具类
 * 使用 CRC32 算法（仅用于内容变化检测，不需要加密）
 */
public final class HashUtil {
    private static final String EMPTY_HASH = "00000000";

    private HashUtil() {
        throw new AssertionError("HashUtil 类不应被实例化");
    }

    /**
     * 计算字符串的 CRC32 哈希值
     * @param value 输入字符串
     * @return 8 位十六进制小写字符串
     */
    public static String crc32(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY_HASH;
        }

        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes(StandardCharsets.UTF_8));
        return formatHash(crc32.getValue());
    }

    /**
     * 计算字符串列表的 CRC32 哈希值
     * @param values 字符串列表
     * @return 8 位十六进制小写字符串
     */
    public static String crc32(List<String> values) {
        if (values == null || values.isEmpty()) {
            return EMPTY_HASH;
        }

        CRC32 crc32 = new CRC32();
        for (String value : values) {
            if (value != null) {
                crc32.update(value.getBytes(StandardCharsets.UTF_8));
            }
        }
        return formatHash(crc32.getValue());
    }

    /**
     * 计算字节数组的 CRC32 哈希值
     * @param bytes 字节数组
     * @return 8 位十六进制小写字符串
     */
    public static String crc32(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return EMPTY_HASH;
        }

        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return formatHash(crc32.getValue());
    }

    /**
     * 格式化哈希值为 8 位十六进制小写字符串
     */
    private static String formatHash(long hash) {
        String hex = Long.toHexString(hash);
        StringBuilder sb = new StringBuilder(8);
        for (int i = hex.length(); i < 8; i++) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString().toLowerCase();
    }
}