package com.lakeserl.payment_service.gateway.vnpay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * VNPay HMAC-SHA512 signature utility.
 * <p>
 * Algorithm (from official VNPay docs):
 * <ol>
 *   <li>Collect all params except {@code vnp_SecureHash} / {@code vnp_SecureHashType}</li>
 *   <li>Sort keys alphabetically</li>
 *   <li>Build {@code key=value&key2=value2...} string (URL-encoded values)</li>
 *   <li>HMAC-SHA512(hashSecret, data)</li>
 * </ol>
 * <p>
 * Verification uses {@link MessageDigest#isEqual(byte[], byte[])} for timing-safe
 * comparison — NEVER {@link String#equals(Object)}.
 */
public final class VNPaySignatureUtil {

    private static final String HMAC_SHA512 = "HmacSHA512";

    private VNPaySignatureUtil() {
    }

    /**
     * Compute HMAC-SHA512 over {@code data} using {@code key}.
     *
     * @return lowercase hex string, or empty string on error
     */
    public static String hmacSHA512(String key, String data) {
        if (key == null || data == null) {
            return "";
        }
        try {
            Mac hmac512 = Mac.getInstance(HMAC_SHA512);
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKeyBytes, HMAC_SHA512);
            hmac512.init(secretKeySpec);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
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
     * Build the hash data string from VNPay params (sorted keys, URL-encoded values)
     * excluding {@code vnp_SecureHash} and {@code vnp_SecureHashType}.
     */
    public static String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            if ("vnp_SecureHash".equals(fieldName) || "vnp_SecureHashType".equals(fieldName)) {
                continue;
            }
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(fieldName).append('=').append(fieldValue);
                first = false;
            }
        }
        return sb.toString();
    }

    /**
     * Verify VNPay webhook signature.
     * <p>
     * Timing-safe comparison via {@link MessageDigest#isEqual}.
     *
     * @param params    all query parameters from the webhook including {@code vnp_SecureHash}
     * @param secretKey the VNPay hash secret
     * @return true if signature is valid
     */
    public static boolean verify(Map<String, String> params, String secretKey) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }
        String hashData = buildHashData(params);
        String computedHash = hmacSHA512(secretKey, hashData);
        if (computedHash.isEmpty()) {
            return false;
        }
        // Timing-safe comparison — NEVER String.equals()
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                receivedHash.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
