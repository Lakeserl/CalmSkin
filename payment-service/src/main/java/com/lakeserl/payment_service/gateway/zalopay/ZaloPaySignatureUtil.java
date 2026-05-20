package com.lakeserl.payment_service.gateway.zalopay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * ZaloPay HMAC-SHA256 signature utility.
 * <p>
 * ZaloPay uses two keys:
 * <ul>
 *   <li>{@code key1} — for creating payment signatures:
 *       {@code MAC = HMAC-SHA256(key1, app_id|app_trans_id|app_user|amount|app_time|embed_data|item)}</li>
 *   <li>{@code key2} — for verifying callback signatures:
 *       {@code MAC = HMAC-SHA256(key2, callback_data)}</li>
 * </ul>
 * Verification uses {@link MessageDigest#isEqual(byte[], byte[])} — timing-safe.
 */
public final class ZaloPaySignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private ZaloPaySignatureUtil() {
    }

    /**
     * Compute HMAC-SHA256 over {@code data} using {@code key}.
     *
     * @return lowercase hex string, or empty string on error
     */
    public static String hmacSHA256(String key, String data) {
        if (key == null || data == null) {
            return "";
        }
        try {
            Mac hmac256 = Mac.getInstance(HMAC_SHA256);
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, HMAC_SHA256);
            hmac256.init(secretKeySpec);
            byte[] result = hmac256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Build the pipe-delimited MAC data for ZaloPay payment creation.
     * Format: {@code app_id|app_trans_id|app_user|amount|app_time|embed_data|item}
     */
    public static String buildCreateMacData(
            String appId, String appTransId, String appUser,
            Long amount, long appTime, String embedData, String item) {
        return appId + "|" + appTransId + "|" + appUser + "|" + amount
                + "|" + appTime + "|" + (embedData != null ? embedData : "")
                + "|" + (item != null ? item : "[]");
    }

    /**
     * Timing-safe verification of an HMAC-SHA256 signature.
     */
    public static boolean verify(String expectedMac, String key, String data) {
        if (expectedMac == null || expectedMac.isEmpty()) {
            return false;
        }
        String computed = hmacSHA256(key, data);
        if (computed.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                expectedMac.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
